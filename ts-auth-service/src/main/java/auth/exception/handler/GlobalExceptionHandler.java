package auth.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import auth.exception.UserOperationException;

/**
 * @author fdse
 */
@ControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler(UserOperationException.class)
    @ResponseBody
    public ResponseEntity<String> handleUserNotFoundException(UserOperationException e)
    {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
