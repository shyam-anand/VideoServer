package com.shyamanand.fileupload.web.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.hateoas.ResourceSupport;

/**
 * @author Shyam Anand (shyamwdr@gmail.com)
 *         04/09/17
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiResponse<T> extends ResourceSupport {
    private T data;
    private ErrorDetails error;

    public ApiResponse(ErrorDetails error) {
        this.error = error;
    }

    public ApiResponse(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
    }
}
