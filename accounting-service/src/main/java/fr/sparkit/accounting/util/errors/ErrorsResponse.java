package fr.sparkit.accounting.util.errors;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ErrorsResponse implements Serializable {


    private static final long serialVersionUID = -209297809537577798L;

    private transient List<Object> errors;

    public ErrorsResponse() {
        this.errors = new ArrayList<>();
    }

    public ErrorsResponse error(Object error) {
        errors.add(error);
        return this;
    }

}
