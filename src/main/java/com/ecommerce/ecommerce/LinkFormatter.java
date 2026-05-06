package com.ecommerce.ecommerce;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LinkFormatter {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<]+");

    public String format(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String escaped = HtmlUtils.htmlEscape(value);
        Matcher matcher = URL_PATTERN.matcher(escaped);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String url = trimTrailingPunctuation(matcher.group());
            String trailing = matcher.group().substring(url.length());
            String link = "<a href=\"" + url + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + url + "</a>" + trailing;
            matcher.appendReplacement(result, Matcher.quoteReplacement(link));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String trimTrailingPunctuation(String url) {
        while (url.endsWith(".") || url.endsWith(",") || url.endsWith(")") || url.endsWith("]")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
