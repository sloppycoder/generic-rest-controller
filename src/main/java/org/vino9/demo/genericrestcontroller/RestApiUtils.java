package org.vino9.demo.genericrestcontroller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.Map;

import static org.vino9.demo.genericrestcontroller.RestApiConstants.PAGE_NO;
import static org.vino9.demo.genericrestcontroller.RestApiConstants.PAGE_SIZE;

public class RestApiUtils {

    static public int getParamAsInt(Map<String, String> params, String key, int defaultValue) {
        return params.containsKey(key) ? Integer.valueOf(params.get(key)).intValue() : defaultValue;
    }

    static public long getParamAsLong(Map<String, String> params, String key, long defaultValue) {
        return params.containsKey(key) ? Long.valueOf(params.get(key)).longValue() : defaultValue;
    }

    static public Pageable getPageableFromParams(Map<String, String> params, int defaultPageSize) {
        int page = getParamAsInt(params, PAGE_NO, 0);
        int perPage = getParamAsInt(params, PAGE_SIZE, defaultPageSize);

        return PageRequest.of(page, perPage);
    }

    static public Map<String, Object> paginationMeta(Page<?> currPage, String url) {
        HashMap<String, Object> meta = new HashMap<>();

        meta.put(PAGE_NO, currPage.getNumber());
        meta.put(PAGE_SIZE, currPage.getSize());
        meta.put("curr", getUrlForPageable(currPage.getPageable(), url));

        if (currPage.hasPrevious()) {
            meta.put("prev", getUrlForPageable(currPage.previousPageable(), url));
        }

        if (currPage.hasNext()) {
            meta.put("next", getUrlForPageable(currPage.nextPageable(), url));
        }

        return meta;
    }

    static public String getUrlForPageable(Pageable pageable, String url) {
        return String.format("%s?page=%d&per_page=%d", url, pageable.getPageNumber(), pageable.getPageSize());
    }
}