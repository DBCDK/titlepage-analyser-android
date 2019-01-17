package dk.dbc.titlepages;

import com.annimon.stream.Optional;

public class ResultHolder<T> {
    private final Optional<T> object;
    private final Optional<Throwable> error;

    ResultHolder() {
        object = Optional.empty();
        error = Optional.empty();
    }

    ResultHolder(T object) {
        this.object = Optional.of(object);
        error = Optional.empty();
    }

    ResultHolder(Throwable error) {
        this.error = Optional.of(error);
        object = Optional.empty();
    }

    Optional<T> getObject() {
        return object;
    }

    Optional<Throwable> getError() {
        return error;
    }
}
