package org.vino9.demo.genericrestcontroller;

public interface IdConverter<ID> {
    ID convert(String id);
}
