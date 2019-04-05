package ng.upperlink.nibss.cmms.mandates.utils;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/*
* Creates a unique mandate code for each mandate generated on CMMS
*
* */
public class MandateUtils {

    public static synchronized String getMandateCode(String num,String billerNo, String productId){

        num = num.substring(5);
        List<String> maxNumList = new ArrayList<>();
        maxNumList.add(num);

        billerNo= StringUtils.leftPad(billerNo, 5, "0");
        productId= StringUtils.leftPad(productId, 3, "0");

        if(maxNumList !=null && maxNumList.size()>0 && maxNumList.get(0)!=null){

            return billerNo+"/"+productId+"/"+StringUtils.leftPad(String.valueOf(maxNumList.stream().findFirst().get()), 6, "0");
        }
        else return null;
    }

}
