package com.cloud.notification.tuna.fastjson.serializer;

import com.cloud.notification.tuna.fastjson.parser.DefaultJSONParser;
import com.cloud.notification.tuna.fastjson.parser.JSONToken;
import com.cloud.notification.tuna.fastjson.parser.deserializer.ObjectDeserializer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;

public class FileCodec implements ObjectSerializer, ObjectDeserializer {

    public static FileCodec instance = new FileCodec();

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.getWriter();
        
        if (object == null) {
            out.writeNull();
            return;
        }
        
        File file = (File) object;
        
        serializer.write(file.getPath());
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialze(DefaultJSONParser parser, Type clazz, Object fieldName) {
        Object value = parser.parse();

        if (value == null) {
            return null;
        }
        
        String path = (String) value;
        
        return (T) new File(path);
    }

    public int getFastMatchToken() {
        return JSONToken.LITERAL_STRING;
    }
}
