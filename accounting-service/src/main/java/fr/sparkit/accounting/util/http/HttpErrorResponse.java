package fr.sparkit.accounting.util.http;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import fr.sparkit.accounting.util.errors.ErrorsResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(Include.NON_EMPTY)
public class HttpErrorResponse {

    private int errorCode;
    private List<Object> errors;

    public HttpErrorResponse(int errorCode, ErrorsResponse errorsResponse) {
        super();
        this.errorCode = errorCode;
        this.errors = errorsResponse.getErrors();
    }

    public HttpErrorResponse(int errorCode) {
        super();
        this.errorCode = errorCode;
        this.errors = new ArrayList<>();
    }

}
