package com.cloud.notification.tuna.fastjson.parser.deserializer;

import com.cloud.notification.tuna.fastjson.JSONException;
import com.cloud.notification.tuna.fastjson.parser.DefaultJSONParser;
import com.cloud.notification.tuna.fastjson.parser.JSONToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;

public class DateFormatDeserializer extends AbstractDateDeserializer implements ObjectDeserializer {

    public final static DateFormatDeserializer instance = new DateFormatDeserializer();

    @SuppressWarnings("unchecked")
    protected <T> T cast(DefaultJSONParser parser, Type clazz, Object fieldName, Object val) {
        
        if (val == null) {
            return null;
        }

        if (val instanceof String) {
            String strVal = (String) val;
            if (strVal.length() == 0) {
                return null;
            }
            
            return (T) new SimpleDateFormat(strVal);
        }

        throw new JSONException("parse error");
    }

    public int getFastMatchToken() {
        return JSONToken.LITERAL_STRING;
    }
}
