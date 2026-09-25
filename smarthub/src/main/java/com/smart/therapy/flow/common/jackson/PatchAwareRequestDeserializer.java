package com.smart.therapy.flow.common.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smart.therapy.flow.common.dto.PatchAwareRequest;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Tracks JSON property names on {@link PatchAwareRequest} subclasses so services can distinguish
 * omitted fields from explicitly null values.
 *
 * <p>Uses the standard bean deserializer as a delegate. Calling {@code readTreeAsValue(targetClass)}
 * would re-enter this same {@code @JsonDeserialize} handler and stack-overflow.
 */
public class PatchAwareRequestDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private final JavaType targetType;
    private final JsonDeserializer<Object> delegate;

    public PatchAwareRequestDeserializer() {
        this.targetType = null;
        this.delegate = null;
    }

    private PatchAwareRequestDeserializer(JavaType targetType, JsonDeserializer<Object> delegate) {
        this.targetType = targetType;
        this.delegate = delegate;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
            throws JsonMappingException {
        JavaType type = targetType;
        if (type == null) {
            type = property != null ? property.getType() : ctxt.getContextualType();
        }
        if (type == null) {
            return this;
        }

        // Build the normal bean deserializer directly so @JsonDeserialize on the class
        // does not select this wrapper again (which would recurse forever).
        BeanDescription beanDesc = ctxt.getConfig().introspect(type);
        JsonDeserializer<Object> beanDeserializer =
                ctxt.getFactory().createBeanDeserializer(ctxt, type, beanDesc);
        if (beanDeserializer instanceof ResolvableDeserializer resolvable) {
            resolvable.resolve(ctxt);
        }
        if (beanDeserializer instanceof ContextualDeserializer contextual) {
            @SuppressWarnings("unchecked")
            JsonDeserializer<Object> contextualized =
                    (JsonDeserializer<Object>) contextual.createContextual(ctxt, property);
            beanDeserializer = contextualized;
        }
        return new PatchAwareRequestDeserializer(type, beanDeserializer);
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        if (delegate == null || targetType == null) {
            return ctxt.readValue(parser, Object.class);
        }

        JsonNode node = parser.getCodec().readTree(parser);
        JsonParser treeParser = node.traverse(parser.getCodec());
        if (treeParser.currentToken() == null) {
            treeParser.nextToken();
        }

        Object instance = delegate.deserialize(treeParser, ctxt);
        if (instance instanceof PatchAwareRequest patchAware && node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                patchAware.markFieldPresent(fields.next().getKey());
            }
        }
        return instance;
    }
}
