package ng.upperlink.nibss.cmms.mandates.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import ng.upperlink.nibss.cmms.dto.UserDetail;
import ng.upperlink.nibss.cmms.dto.mandates.MandateFrequencyResponse;
import ng.upperlink.nibss.cmms.enums.*;
import ng.upperlink.nibss.cmms.mandates.exceptions.ExcelReaderException;
import ng.upperlink.nibss.cmms.model.User;
import ng.upperlink.nibss.cmms.model.bank.Bank;
import ng.upperlink.nibss.cmms.model.biller.Biller;
import ng.upperlink.nibss.cmms.model.biller.Product;
import ng.upperlink.nibss.cmms.model.mandate.BulkMandate;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.model.mandate.MandateStatus;
import ng.upperlink.nibss.cmms.service.FormValidation;
import ng.upperlink.nibss.cmms.service.bank.BankService;
import ng.upperlink.nibss.cmms.service.biller.ProductService;
import ng.upperlink.nibss.cmms.service.mandateImpl.BulkMandateService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateStatusService;
import ng.upperlink.nibss.cmms.util.AccountLookUp;
import ng.upperlink.nibss.cmms.util.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.DVConstraint;
import org.apache.poi.hssf.usermodel.HSSFDataValidation;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
* Handles parsing Excel files
* */
@Service
public class ExcelParser {
    private ProductService productService;
    private BankService bankService;
    private FormValidation formValidation;
    private AccountLookUp accountLookUp;
    private final static String EXCEL_FILE_NAME = "template.xls";
    private MandateService mandateService;
    private MandateStatusService mandateStatusService;
    private BulkMandateService bulkMandateService;

    @Value("${excel.file.path}")
    private String excelFilePath;

    private static final Logger logger = LoggerFactory.getLogger(ExcelParser.class);

    @Autowired
    public void setBulkMandateService(BulkMandateService bulkMandateService) {
        this.bulkMandateService = bulkMandateService;
    }

    @Autowired
    public void setMandateStatusService(MandateStatusService mandateStatusService) {
        this.mandateStatusService = mandateStatusService;
    }

    @Lazy
    @Autowired
    private void setMandateService(MandateService mandateService){
        this.mandateService = mandateService;
    }

    @Autowired
    public void setAccountLookUp(AccountLookUp accountLookUp) {
        this.accountLookUp = accountLookUp;
    }

    @Autowired
    public void setFormValidation(FormValidation formValidation) {
        this.formValidation = formValidation;
    }

    @Autowired
    public void setBankService(BankService bankService) {
        this.bankService = bankService;
    }

    @Autowired
    public void setProductService(ProductService productService) {
        this.productService = productService;
    }


    /** Generate and return an empty excel file
     * @param userDetail
     * @param biller
     * @return
     * @throws Exception
     */
    public ResponseEntity<InputStreamResource> getGeneratedExcelFile(UserDetail userDetail, Biller biller) throws Exception {
        File excelFile = generateExcelFile(userDetail, biller);

        return ResponseEntity
                .ok()
                .contentLength(new Double(excelFile.length()).longValue())
                .contentType(
                        MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(new InputStreamResource(new ByteArrayInputStream(Files.toByteArray(excelFile))));
    }


    /** Generate excel file
     * @param userDetail
     * @param biller
     * @return
     * @throws Exception
     */
    public File generateExcelFile(UserDetail userDetail, Biller biller) throws Exception {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        createHeaderRowAndDropDownConstants(workbook, sheet, biller);

        createAllCustomCell(workbook);

       // File file = new File("C:\\ExcelDirectory\\" + EXCEL_FILE_NAME);
        File file = new File(excelFilePath + EXCEL_FILE_NAME);
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }

        return file;
    }


    /** Create headers and dropdowns for excel sheet
     * @param workbook
     * @param sheet
     * @param biller
     * @throws Exception
     */
    public void createHeaderRowAndDropDownConstants(Workbook workbook, Sheet sheet, Biller biller) throws Exception {

        CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        cellStyle.setFont(font);

        Row row = sheet.createRow(0);
        row.setRowStyle(cellStyle);
        setTitle(row);

        writeDropDown(workbook, sheet, biller);

    }

    //set the title for each column of the excel sheet
    public void setTitle(Row row) {

        row.createCell(1).setCellValue("SUBSCRIBER CODE");
        row.createCell(2).setCellValue("PAYER NAME");
        row.createCell(3).setCellValue("EMAIL");
        row.createCell(4).setCellValue("ACCOUNT NUMBER");
        row.createCell(5).setCellValue("ACCOUNT NAME");
        row.createCell(6).setCellValue("PAYER ADDRESS");
        row.createCell(7).setCellValue("PHONE NUMBER");
        row.createCell(8).setCellValue("BVN");
        row.createCell(9).setCellValue("START DATE");
        row.createCell(10).setCellValue("END DATE");
        row.createCell(11).setCellValue("SUBSCRIBER BANK");
        row.createCell(12).setCellValue("BILLER");
        row.createCell(13).setCellValue("PRODUCT");
        row.createCell(14).setCellValue("FREQUENCY");
        row.createCell(15).setCellValue("NARRATION");
        row.createCell(16).setCellValue("CHANNEL");
        row.createCell(17).setCellValue("MANDATE TYPE");
        row.createCell(18).setCellValue("MANDATE CATEGORY");
        row.createCell(19).setCellValue("FIXED AMOUNT");
        //row.createCell(20).setCellValue("VARIABLE AMOUNT");
    }

    public void createAllCustomCell(Workbook workbook) {
        setCellAsString(workbook, 1,false);
        setCellAsString(workbook, 2,false);
        setCellAsString(workbook, 3,false);
        setCellAsString(workbook, 4,false);
        setCellAsString(workbook, 5,false);
        setCellAsString(workbook, 6,false);
        setCellAsString(workbook, 7,false);
        setCellAsString(workbook, 8,false);
        setCellAsString(workbook, 9,false);  //mandate start date
        setCellAsString(workbook, 10,false);  //mandate end date
        setCellAsString(workbook, 11,false);
        setCellAsString(workbook, 12,false);
        setCellAsString(workbook, 13,false);
        setCellAsString(workbook, 14,false);
        setCellAsString(workbook, 15,false);
        setCellAsString(workbook, 16,false);
        setCellAsString(workbook, 17,false);
        setCellAsString(workbook, 18,false);
        setCellAsString(workbook, 19,false);
        //setCellAsString(workbook, 20,false);
    }

    public void setCellAsString(Workbook workbook, int cellNumber,boolean cellTypeFlag) {

        Sheet sheetAt = workbook.getSheetAt(0);

        DataFormat fmt = workbook.createDataFormat();
        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setDataFormat(fmt.getFormat("@"));
        //1000 rows starting from row 1
        for (int i = 1; i < 10; i++) {
            Cell cell = sheetAt.createRow(i).createCell(cellNumber);
            cell.setCellType(CellType.STRING);

            sheetAt.setDefaultColumnStyle(cellNumber, textStyle);
        }

    }

    public void setCellAsDate(Workbook workbook, int cellNumber) {
        Sheet sheetAt = workbook.getSheetAt(0);

        CellStyle cellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();

        // Set the date format of date
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

        //1000 rows starting from row 1
        for (int i = 1; i < 10; i++) {
            Cell cell = sheetAt.createRow(i).createCell(cellNumber);
            cell.setCellStyle(cellStyle);
        }

    }


    /** Add the dropdown columns to the excel file
     * @param workbook
     * @param sheet
     * @param biller
     * @throws Exception
     */
    public void writeDropDown(Workbook workbook, Sheet sheet, Biller biller) throws Exception {

        //get the products that belong to the biller
        List<String> products = productService.getAllActiveProductsByBiller(true, biller.getId()).stream().map(r -> r.getName()).collect(Collectors.toList());
        List<String> freq = MandateFrequency.getMandateFrequencies().stream().map(m -> m.getDescription()).collect(Collectors.toList());
        List<String> banks = bankService.getAllActivated(true).stream().map(Bank::getName).collect(Collectors.toList());
        Channel portalChannel = Channel.PORTAL;
        List<String> channels = Arrays.asList(portalChannel.getValue());

        MandateRequestType mType = MandateRequestType.FIXED;
        List<String> mandateTypes = Arrays.asList(mType.getValue());

        MandateCategory mc = MandateCategory.PAPER;
        List<String> categories = Arrays.asList(mc.getValue());


        dropDownList(workbook, sheet, banks, 11, "banks");
        dropDownList(workbook, sheet, Arrays.asList(biller.getName()), 12, "biller");
        dropDownList(workbook, sheet, products, 13, "products");
        dropDownList(workbook, sheet, freq, 14, "frequency");
        dropDownList(workbook, sheet, channels, 16, "channel");
        dropDownList(workbook, sheet, mandateTypes, 17, "mandateType");
        dropDownList(workbook, sheet, categories, 18, "mandateCategory");
    }

    public void dropDownList(Workbook workbook, Sheet sheet, List<String> contents, int cellNumber, String typeName) {
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, cellNumber, cellNumber);
        if (contents.size() == 0)
            contents.add("NA");

        int size = contents.size();
        //create another sheet
        Sheet newSheet = workbook.createSheet(typeName);
        for (int i = 0; i < size; i++) {
            Row row = newSheet.createRow(i);
            row.createCell(0).setCellValue(contents.get(i));
        }

        //Use namedCell
        Name namedCell = workbook.createName();

        namedCell.setNameName(typeName + "1");
        namedCell.setRefersToFormula("'" + typeName + "'!$A$1:$A$" + size);
        DVConstraint dvConstraint = DVConstraint.createFormulaListConstraint(typeName + "1");

        DataValidation dataValidation = new HSSFDataValidation(addressList, dvConstraint);

        dataValidation.setSuppressDropDownArrow(false);
        sheet.addValidationData(dataValidation);

        //hide the sheet
        workbook.setSheetHidden(workbook.getSheetIndex(newSheet), true);
    }

    //save bulk mandates
    public List<Mandate> saveBulk(List<Mandate> bulkObject) throws Exception {
        List<Mandate> listOfMandates = new ArrayList<>();
        List<BulkMandate> bulkMandateList = new ArrayList<>();
        BulkMandate bulkMandate = new BulkMandate();
        ObjectMapper mapper = new ObjectMapper();
        for (Object mandate : bulkObject) {
            listOfMandates.add(Mandate.class.cast(mandate));
        }

        //return (List<T>)mandateService.saveBulkMandate(listOfMandates);

        Long maxId = null;
        try{
            maxId = mandateService.getMaxMandate() + 1;
        }catch(NullPointerException ex){
            maxId = 1L;
        }

        if(!listOfMandates.isEmpty()) {
            for(Mandate m : listOfMandates){
                m.setId(maxId);
                String mandateString = mapper.writeValueAsString(m);
                bulkMandate.setMandateId(m.getId());
                bulkMandate.setMandateInJson(mandateString);
                bulkMandateList.add(bulkMandate);

                maxId++;
            }
        }

        bulkMandateService.saveBulkMandate(bulkMandateList);

        return listOfMandates;
    }

    //perform save operation after retrieving contents from excel sheet
    public  List<Mandate> generateAndSaveObjects(Workbook workbook, int totalRowCount, Object object,
                                                 Biller biller,String userRole) throws Exception {

        //then iterate through the rows to get the items and also validate
        List<Mandate> mandateList = fetchFromExcelSheet(workbook, totalRowCount, object, biller,userRole);

        //bulk save
        mandateList = saveBulk(mandateList);

        return mandateList;
    }

    //Retrieve contents from excel sheet
    public <T> List<T> fetchFromExcelSheet(Workbook workbook, int totalRowCount, Object user, Biller biller,String userRole) throws Exception {

        Sheet sheet1 = workbook.getSheetAt(0);

        //get the Person who performed this operation.
        User userOperator = (User) user;


        List<Mandate> mandateList = new ArrayList<>();
//
        //get HashMap of Banks
        Map<String, Bank> bankMap = new HashMap<>();
        List<Bank> banks = bankService.getAllActivated(true);
        banks.forEach(bank -> bankMap.put(bank.getName(), bank));

        //get HashMap of Billers
        Map<String, Biller> billerMap = new HashMap<>();
        List<Biller> billerList = Arrays.asList(biller);
        billerList.forEach(b -> billerMap.put(b.getName(), b));

        //get HashMap of Products
        Map<String, Product> productMap = new HashMap<>();
        List<Product> productList = productService.getAllActiveProductsByBiller(true, biller.getId());
        productList.forEach(pr -> productMap.put(pr.getName(), pr));

        //get HashMap of Frequencies
        Map<String, MandateFrequencyResponse> frequencyMap = new HashMap<>();
        List<MandateFrequencyResponse> freqList = MandateFrequency.getMandateFrequencies();
        freqList.forEach(fre -> frequencyMap.put(fre.getDescription(), fre));

        //get HashMap of MandateTypes
        Map<String, MandateRequestType> mandateTypeMap = new HashMap<>();
        List<String> typeList = Stream.of(MandateRequestType.values()).map(MandateRequestType::getValue).collect(Collectors.toList());
        typeList.forEach(type -> mandateTypeMap.put(type, MandateRequestType.findByValue(type)));

        //get HashMap of MandateCategories
        Map<String, MandateCategory> mandateCategoryMap = new HashMap<>();
        List<String> categoryList = Stream.of(MandateCategory.values()).map(MandateCategory::getValue).collect(Collectors.toList());
        categoryList.forEach(category -> mandateCategoryMap.put(category, MandateCategory.findByValue(category)));

               //iterate through the rows in the excel workbook
        for (Row row : sheet1) {
            //if it is the title then skip
            if (sheet1.getRow(0) == row) {
                continue;
            }

            if (row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL) == null) {
                continue;
            }

            //count the number of mandate filled by user.
            ++totalRowCount;

            Mandate mandate = new Mandate();
            //int errorCellValue = 22;

            //1. SUBSCRIBER CODE
            String subscriberCode = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();


            //2. PAYER NAME
            String payerName = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();

            //3. EMAIL ADDRESS
            String emailAddressString = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();

            if (!formValidation.validEmail(emailAddressString)) {
                throw new ExcelReaderException("Invalid email address at row " + row.getRowNum() + " and cell " + row.getCell(3).getAddress().formatAsString());
            }

            //4. ACCOUNT NUMBER - validate account number
            String accountNumber = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();

            if (!accountLookUp.validateAccount(accountNumber, Constants.ACC_NUMBER_MAX_DIGITS)) {
                throw new ExcelReaderException(String.format("Account number must be digits and not less than %d characters at row %s and cell %s", Constants.ACC_NUMBER_MAX_DIGITS, row.getRowNum(), row.getCell(4).getAddress().formatAsString()));
            }

            //5. ACOUNT NAME
            String accountName = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();

            //6. PAYER ADDRESS
            String payerAddress = row.getCell(6, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();

            //7. PHONE NUMBER - validated if provided
            String phoneNumberString = row.getCell(7, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();

            if (phoneNumberString != null && !phoneNumberString.isEmpty() && !formValidation.validPhoneNumber(phoneNumberString)) {
                throw new ExcelReaderException(String.format("Invalid phone number at row %s and cell %s", row.getRowNum(), row.getCell(7).getAddress().formatAsString()));
            }

            //8. BVN - must be 11 digits if provided
            String bvnString = null;
            try {
                bvnString = row.getCell(8, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
            } catch (Exception e) {
                bvnString = String.format("%.0f", Double.valueOf(row.getCell(8, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getNumericCellValue()));
            }
            if (bvnString != null && !bvnString.isEmpty() && !accountLookUp.validateAccount(bvnString, Constants.BVN_MAX_DIGITS)) {
                throw new ExcelReaderException(String.format("Bvn must be digits and not less than %d characters at row %s and cell %s", Constants.BVN_MAX_DIGITS, row.getRowNum(), row.getCell(8).getAddress().formatAsString()));
            }

            //9. START DATE
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);
            int second = now.get(Calendar.SECOND);
            Date startDateFormatted = null;
            String startDate = row.getCell(9, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue() + " " + (String.format("%02d:%02d:%02d",hour, minute, second));;
            try {
                startDateFormatted = dateFormat.parse(startDate);
            } catch (ParseException ex) {
                throw new ExcelReaderException(String.format("Invalid date supplied at row %s and cell %s", row.getRowNum(), row.getCell(9).getAddress().formatAsString()));
            }

            if (startDateFormatted == null || StringUtils.isEmpty(startDate)) {
                throw new ExcelReaderException("Start Date not provided at row " + row.getRowNum() + " and cell " + row.getCell(9).getAddress().formatAsString());
            }

            //10. END DATE
            Date endDateFormatted = null;
            String endDate = row.getCell(10, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue() + " " + (String.format("%02d:%02d:%02d",hour, minute, second));;
            try {
                endDateFormatted = dateFormat.parse(endDate);
            } catch (ParseException ex) {
                throw new ExcelReaderException(String.format("Invalid date supplied at row %s and cell %s", row.getRowNum(), row.getCell(10).getAddress().formatAsString()));
            }

            if (endDateFormatted == null || StringUtils.isEmpty(endDate)) {
                throw new ExcelReaderException("End Date not provided at row " + row.getRowNum() + " and cell " + row.getCell(10).getAddress().formatAsString());
            }


            //11. BANK drop down
            String bankName = row.getCell(11, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
            Bank bank = bankMap.get(bankName);
            if (bank == null) {
                throw new ExcelReaderException(String.format("Invalid bank at row %s and cell %s", row.getRowNum(), row.getCell(11).getAddress().formatAsString()));
            }

            //12. BILLER drop down
            String billerName = row.getCell(12, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
            Biller billerInExcel = billerMap.get(billerName);
            if (billerInExcel == null) {
                throw new ExcelReaderException(String.format("Invalid biller at row %s and cell %s", row.getRowNum(), row.getCell(12).getAddress().formatAsString()));
            }

            //13. PRODUCT drop down
            String productName = row.getCell(13, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
            Product product = productMap.get(productName);
            if (product == null) {
                throw new ExcelReaderException(String.format("Invalid product at row %s and cell %s", row.getRowNum(), row.getCell(13).getAddress().formatAsString()));
            }

            //14. FREQUENCY drop down
            String frequencyDesc = row.getCell(14, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
            MandateFrequencyResponse mandateFreq = frequencyMap.get(frequencyDesc);
            if (mandateFreq == null) {
                throw new ExcelReaderException(String.format("Invalid frequency at row %s and cell %s", row.getRowNum(), row.getCell(14).getAddress().formatAsString()));
            }

            //15. NARRATION
            String narration = row.getCell(15, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();

            //16. CHANNEL
            String channelString = row.getCell(16, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();

            //17. MANDATE TYPE
            String mandateTypeString = row.getCell(17, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
            MandateRequestType mandateType = mandateTypeMap.get(mandateTypeString);
            if (mandateType == null) {
                throw new ExcelReaderException(String.format("Invalid mandate type at row %s and cell %s", row.getRowNum(), row.getCell(17).getAddress().formatAsString()));
            }

            //18. MANDATE CATEGORY
            String mandateCategoryString = row.getCell(18, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
            MandateCategory mandateCategory = mandateCategoryMap.get(mandateCategoryString);
            if (mandateCategory == null) {
                throw new ExcelReaderException(String.format("Invalid mandate category at row %s and cell %s", row.getRowNum(), row.getCell(18).getAddress().formatAsString()));
            }

            //19. FIXED AMOUNT
            BigDecimal fixedAmt = null;
            try {
                Double amt = row.getCell(19, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getNumericCellValue();
                fixedAmt = new BigDecimal(amt);
            } catch (IllegalStateException e) {
                throw new ExcelReaderException(String.format("Invalid amount at row %s and cell %s", row.getRowNum(), row.getCell(19).getAddress().formatAsString()));
            } catch (NumberFormatException ex) {
                throw new ExcelReaderException(String.format("Invalid amount at row %s and cell %s", row.getRowNum(), row.getCell(19).getAddress().formatAsString()));
            }

            //20. VARIABLE AMOUNT
//            BigDecimal variableAmount = null;
//            try {
//                Double varAmt = row.getCell(20, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getNumericCellValue();
//            System.out.println(varAmt);
//                variableAmount = new BigDecimal(varAmt);
//            } catch (IllegalStateException e) {
//                throw new ExcelReaderException(String.format("Invalid amount at row %s and cell %s", row.getRowNum(), row.getCell(20).getAddress().formatAsString()));
//            } catch (NumberFormatException ex) {
//                throw new ExcelReaderException(String.format("Invalid amount at row %s and cell %s", row.getRowNum(), row.getCell(20).getAddress().formatAsString()));
//            }


            //validate startdate, end date and frequency
            this.validate(mandateFreq.getId(),startDateFormatted,endDateFormatted,row);

            mandate.setSubscriberCode(subscriberCode);
            mandate.setPayerName(payerName);
            mandate.setEmail(emailAddressString);
            mandate.setAccountNumber(accountNumber);
            mandate.setAccountName(accountName);
            mandate.setPayerAddress(payerAddress);
            mandate.setPhoneNumber(phoneNumberString);
            mandate.setBvn(bvnString);
            mandate.setStartDate(startDateFormatted);
            mandate.setEndDate(endDateFormatted);
            mandate.setBank(new Bank(bank.getId(), bank.getCode(), bank.getName()));
            mandate.setBiller(billerInExcel);
            mandate.setProduct(product);
            mandate.setFrequency(mandateFreq.getId());
            mandate.setNarration(narration);
            mandate.setChannel(Channel.find(channelString));
            mandate.setMandateCategory(mandateCategory);
            mandate.setAmount(fixedAmt);
           // mandate.setVariableAmount(variableAmount);
            mandate.setLastActionBy(new User(userOperator.getId(), userOperator.getName(), userOperator.getEmailAddress(), userOperator.isActivated(), userOperator.getUserType()));
            mandate.setMandateType(mandateType);
            mandate.setMandateCode(billerInExcel != null ? MandateUtils.getMandateCode(String.valueOf(System.currentTimeMillis()), billerInExcel.getRcNumber(), String.valueOf(product.getId())) : null);
            mandate.setScheduleTime(MandateFrequency.findById(mandateFreq.getId()).getDescription());
            mandate.setRequestStatus(Constants.STATUS_ACTIVE);
            mandate.setDateCreated(new Date());
            mandate.setCreatedAt(new Date());
            mandate.setRejection(null);
            mandate.setCreatedBy(new User(userOperator.getId(), userOperator.getName(), userOperator.getEmailAddress(), userOperator.isActivated(), userOperator.getUserType()));
            if (mandate.getFrequency() > 0) {
                Date nextDebitDate = DateUtils.calculateNextDebitDate(mandate.getStartDate(), mandate.getEndDate(),
                        mandate.getFrequency());
                mandate.setNextDebitDate(nextDebitDate == null ? DateUtils.lastSecondOftheDay(mandate.getEndDate())
                        : DateUtils.nullifyTime(nextDebitDate));
            }
                  //set the mandate status and the workflow status
                //Creating new mandates
                MandateStatus mandateStatus = null;
                if (userRole.equals(RoleName.BANK_BILLER_INITIATOR.getValue())) {
                    mandateStatus = mandateStatusService.getMandateStatusById(Constants.BANK_BILLER_INITIATE_MANDATE);
                    //mandateStatus = mandateStatusService.getMandateStatusByStatusName(MandateStatusType.BANK_BILLER_INITIATE_MANDATE);
                    mandate.setStatus(mandateStatus);
                    mandate.setWorkflowStatus(mandateStatus.getName());
                } else if (userRole.equals(RoleName.BILLER_INITIATOR.getValue())) {
                    mandateStatus = mandateStatusService.getMandateStatusById(Constants.BILLER_INITIATE_MANDATE);
                    mandate.setStatus(mandateStatus);
                    mandate.setWorkflowStatus(mandateStatus.getName());
                }


            // if the row is not empty, add the mandate to the list
            boolean check = this.checkIfRowIsEmpty(row);
            if(!check){
                //add to list
                mandateList.add(mandate);
            }

            //send mail to the user containing the password

//            if (totalRowCount > 20) {
//                //Max of 500
//                break;
//            }


        }

        return (List<T>) mandateList;
    }


    /**  Validate file
     * @param file
     * @return
     */
    public String validateFile(MultipartFile file) {

        //get the file
        if (file == null) {
            return "file is null";
        }

        if (!"application/vnd.ms-excel".equalsIgnoreCase(file.getContentType())) {
            return "Not an excel file";
        }

        return null;
    }


    /** Create bulk mandates
     * @param file
     * @param object
     * @param biller
     * @param userRole
     * @return
     * @throws Exception
     */
    public ResponseEntity bulkCreation(MultipartFile file,Object object,
                                                            Biller biller,String userRole) throws Exception {

        String error = validateFile(file);
        String messageName = "message";
        if (error != null) {
            return ResponseEntity.badRequest().header(messageName, error).body(null);
        }

        InputStream inputStream = new BufferedInputStream(file.getInputStream());
        Workbook workbook = WorkbookFactory.create(inputStream);
        int totalRowCount = 0;

        List<Mandate> createdObjects = generateAndSaveObjects(workbook, totalRowCount, object, biller,userRole);

        inputStream.close();

        return ResponseEntity.ok(createdObjects);
    }

    //validate startdate enddate and frequency
    public void validate(int frequency, Date startDate, Date endDate, Row row) {

        //check the dates vs the frequency selected
        if (frequency > 0 && (startDate != null || endDate != null)) {
            try {
                if (startDate.compareTo(DateUtils.nullifyTime(new Date())) <= 0) {
                    throw new ExcelReaderException(String.format("Mandate start date cannot be today or less at row %s !", row.getRowNum()));
                } else {
                    long difference = endDate.getTime() - startDate.getTime();

                    difference = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS);
                    if (difference < (frequency * 7)) {
                        throw new ExcelReaderException(String.format("Mandate date range must be able to accommodate frequency at row %s !", row.getRowNum()));
                    }
                }
            } catch (Exception e) {
                throw new ExcelReaderException(String.format("Unable to compute debit frequency at row %s !", row.getRowNum()));
            }
        } else {
            throw new ExcelReaderException(String.format("Start date, end date and frequency must be provided at row %s !", row.getRowNum()));
        }
    }

    //prevent rowd
    private boolean checkIfRowIsEmpty(Row row){
        if(row == null){
            return true;
        }
        if(row.getLastCellNum() <= 0){
            return true;
        }
        for(int cellNum = row.getFirstCellNum();cellNum < row.getLastCellNum();cellNum++){
            Cell cell = row.getCell(cellNum);
            if(cell != null && cell.getCellTypeEnum() != CellType.BLANK && StringUtils.isNotBlank(cell.toString())){
                return false;
            }
        }
        return true;
    }
}
