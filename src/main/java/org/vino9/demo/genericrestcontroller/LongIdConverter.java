package org.vino9.demo.genericrestcontroller;

public class LongIdConverter implements IdConverter<Long> {
    public Long convert(String id) {
        return Long.valueOf(id);
    }
}
