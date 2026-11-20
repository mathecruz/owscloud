package dev.matheuscruz;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * AWS JSON protocol responses (e.g. DynamoDB) use the {@code application/x-amz-json-1.0} media type,
 * which the REST client used by Quarkus Flow does not map to JSON by default.
 */
@Provider
@Consumes({ "application/x-amz-json-1.0", "application/x-amz-json-1.1" })
public class AmzJsonMessageBodyReader implements MessageBodyReader<Object> {

    @Inject
    ObjectMapper mapper;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return "application".equals(mediaType.getType()) && mediaType.getSubtype().startsWith("x-amz-json");
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        return mapper.readValue(entityStream, mapper.constructType(genericType));
    }
}
