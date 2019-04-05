package ng.upperlink.nibss.cmms.mandates.exceptions;


import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.errorHandler.ErrorDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 */
@Slf4j
@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

	//takes care of exceptions when performing validations
	@ExceptionHandler(ValidationException.class)
	public @ResponseBody Object handleCustomException(ValidationException ve, HttpServletRequest request) {
		log.info("...caught validation exception...");
		
		return new ResponseEntity<ErrorDetails>(new ErrorDetails(ve.getMessage()), HttpStatus.BAD_REQUEST);
 
	}

	//takes care of general exceptions
	@ExceptionHandler(Exception.class)
	public @ResponseBody Object handleGeneralException(HttpServletRequest request, Exception e) throws Exception {
		log.error("Error thrown {} ",e);
		return new ResponseEntity<ErrorDetails>(new ErrorDetails(e.getMessage().toString()), HttpStatus.INTERNAL_SERVER_ERROR);
	}


	@ExceptionHandler(CustomGenericException.class)
	public @ResponseBody Object handleCustomException(HttpServletRequest request, Exception e) throws Exception {
		log.error("Error thrown {} ",e);
		return new ResponseEntity<ErrorDetails>(new ErrorDetails(e.getMessage().toString()), HttpStatus.BAD_REQUEST);
	}

	//handles invalid file exceptions
	@ExceptionHandler(InvalidFileException.class)
	public @ResponseBody Object handleFileException(HttpServletRequest request, Exception e) throws Exception {
		log.error("Invalid file exception thrown {} ",e);
		return new ResponseEntity<ErrorDetails>(new ErrorDetails(e.getMessage().toString()), HttpStatus.BAD_REQUEST);
	}

	//handles exceptions when reading files
	@ExceptionHandler(ExcelReaderException.class)
	public @ResponseBody Object handleExcelFileException(HttpServletRequest request, Exception e) throws Exception {
		log.error("Excel Processing exception thrown {} ",e);
		return new ResponseEntity<ErrorDetails>(new ErrorDetails(e.getMessage().toString()), HttpStatus.BAD_REQUEST);
	}

//	@Override
//	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
//		List<String> details = new ArrayList<>();
//		for(ObjectError error : ex.getBindingResult().getAllErrors()) {
//			details.add(error.getDefaultMessage());
//		}
//		ErrorResponse error = new ErrorResponse("Validation Failed", details);
//		return new ResponseEntity(error, HttpStatus.BAD_REQUEST);
//	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
		List<String> errors = new ArrayList<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.add(error.getDefaultMessage());
		}
		for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
			errors.add(error.getDefaultMessage());
		}

		ErrorDetails errorDetails = new ErrorDetails(new Date(), "Validation Failed", errors);
		return new ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST);
	}

	
}
