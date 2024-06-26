package io.axual.ksml.parser;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 - 2024 Axual B.V.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import io.axual.ksml.data.exception.ParseException;
import io.axual.ksml.data.notation.UserType;
import io.axual.ksml.data.parser.*;
import io.axual.ksml.data.schema.*;
import io.axual.ksml.data.tag.ContextTags;
import io.axual.ksml.exception.TopologyException;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DefinitionParser<T> extends BaseParser<T> implements StructParser<T> {
    public static final String SCHEMA_NAMESPACE = "io.axual.ksml";
    private final DurationParser durationParser = new DurationParser();
    protected final ParserWithSchema<String> codeParser;
    // The global namespace for this definition parser. The namespace is the specified name in the runner configuration
    // for this particular KSML definition. It is used to prefix global names in Kafka Streams (eg. processor names) to
    // ensure no conflict between processor names for two separate definitions.
    private StructParser<T> parser;

    public DefinitionParser() {
        final var codeStringParser = new StringValueParser(value -> value ? "True" : "False");
        final var codeSchema = new UnionSchema(DataSchema.booleanSchema(), DataSchema.stringSchema());
        this.codeParser = ParserWithSchema.of(codeStringParser::parse, codeSchema);
    }

    protected abstract StructParser<T> parser();

    public final T parse(ParseNode node) {
        if (parser == null) parser = parser();
        return parser.parse(node);
    }

    public final StructSchema schema() {
        if (parser == null) parser = parser();
        return parser.schema();
    }

    protected static <V> StructParser<V> optional(StructParser<V> parser) {
        if (parser.fields().isEmpty()) return parser;
        final var schema = parser.schema();
        final var newFields = schema.fields().stream()
                .map(field -> field.required() ? new DataField(field.name(), field.schema(), "*(optional)* " + field.doc(), field.index(), false, field.constant(), field.defaultValue()) : field)
                .toList();
        final var newSchema = new StructSchema(schema.namespace(), schema.name(), schema.doc(), newFields);
        return StructParser.of(node -> node != null ? parser.parse(node) : null, newSchema);
    }

    protected static StructSchema structSchema(Class<?> clazz, String doc, List<DataField> fields) {
        return structSchema(clazz.getSimpleName(), doc, fields);
    }

    protected static StructSchema structSchema(String name, String doc, List<DataField> fields) {
        return new StructSchema(SCHEMA_NAMESPACE, name, doc, fields);
    }

    private static class FieldParser<V> implements StructParser<V>, NamedObjectParser {
        private final DataField field;
        @Getter
        private final StructSchema schema;
        private final Parser<V> valueParser;
        @Getter
        @Setter
        private String defaultName;

        public FieldParser(String childName, boolean constant, V valueIfNull, String doc, ParserWithSchema<V> valueParser) {
            final var defaultValue = valueIfNull != null ? new DataValue(valueIfNull) : null;
            final var docDefault = valueIfNull != null ? " Default: " + valueIfNull : "";
            field = new DataField(childName, valueParser.schema(), doc + docDefault, DataField.NO_INDEX, true, constant, defaultValue);
            schema = structSchema((String) null, null, List.of(field));
            this.valueParser = valueParser;
        }

        @Override
        public V parse(ParseNode node) {
            try {
                if (node == null) return null;
                final var child = node.get(field.name());
                return child != null ? valueParser.parse(child) : null;
            } catch (Exception e) {
                throw new ParseException(node, e.getMessage(), e);
            }
        }
    }

    private static class ValueStructParser<V> implements StructParser<V> {
        @Getter
        private final StructSchema schema;
        private final Parser<V> valueParser;

        public ValueStructParser(String name, String doc, List<StructParser<?>> fields, Parser<V> valueParser) {
            final var structFields = new ArrayList<DataField>();
            fields.forEach(p -> structFields.addAll(p.fields()));
            schema = structSchema(name, doc, structFields);
            this.valueParser = valueParser;
        }

        @Override
        public V parse(ParseNode node) {
            try {
                return valueParser.parse(node);
            } catch (Exception e) {
                throw new ParseException(node, e.getMessage(), e);
            }
        }
    }

    protected <V> StructParser<V> freeField(String childName, V valueIfNull, String doc, ParserWithSchema<V> parser) {
        return new FieldParser<>(childName, false, valueIfNull, doc, parser);
    }

    protected StructParser<Boolean> booleanField(String childName, String doc) {
        return freeField(childName, false, doc, ParserWithSchema.of(ParseNode::asBoolean, DataSchema.booleanSchema()));
    }

    protected StructParser<Duration> durationField(String childName, String doc) {
        return freeField(childName, null, doc, durationParser);
    }

    protected StructParser<Integer> integerField(String childName, String doc) {
        return integerField(childName, null, doc);
    }

    protected StructParser<Integer> integerField(String childName, Integer valueIfNull, String doc) {
        return freeField(childName, valueIfNull, doc, ParserWithSchema.of(ParseNode::asInt, DataSchema.integerSchema()));
    }

    protected StructParser<String> fixedStringField(String childName, String fixedValue, String doc) {
        return stringField(childName, true, fixedValue, doc + ", fixed value \"" + fixedValue + "\"");
    }

    protected StructParser<String> stringField(String childName, String doc) {
        return stringField(childName, null, doc);
    }

    protected StructParser<String> stringField(String childName, String valueIfNull, String doc) {
        return stringField(childName, false, valueIfNull, doc);
    }

    protected StructParser<String> stringField(String childName, boolean constant, String valueIfNull, String doc) {
        return new FieldParser<>(childName, constant, valueIfNull, doc, new StringValueParser());
    }

    protected StructParser<String> codeField(String childName, String doc) {
        return codeField(childName, null, doc);
    }

    protected StructParser<String> codeField(String childName, String valueIfNull, String doc) {
        return new FieldParser<>(childName, false, valueIfNull, doc, codeParser);
    }

    protected StructParser<UserType> userTypeField(String childName, String doc) {
        final var stringParser = stringField(childName, null, doc);
        final var field = new DataField(childName, DataSchema.stringSchema(), doc, DataField.NO_INDEX, true, false, null);
        return new StructParser<>() {
            @Override
            public StructSchema schema() {
                return structSchema((String) null, null, List.of(field));
            }

            @Override
            public UserType parse(ParseNode node) {
                final var type = UserTypeParser.parse(stringParser.parse(node));
                return type != null ? type : UserType.UNKNOWN;
            }
        };
    }

    protected <TYPE> StructParser<List<TYPE>> listField(String childName, String childTagKey, String childTagValuePrefix, String doc, ParserWithSchema<TYPE> valueParser) {
        return new FieldParser<>(childName, false, new ArrayList<>(), doc, new ListWithSchemaParser<>(childTagKey, childTagValuePrefix, valueParser));
    }

    protected <TYPE> StructParser<Map<String, TYPE>> mapField(String childName, String childTagKey, String childTagValuePrefix, String doc, ParserWithSchema<TYPE> valueParser) {
        return new FieldParser<>(childName, false, new HashMap<>(), doc, new MapWithSchemaParser<>(childTagKey, childTagValuePrefix, valueParser));
    }

    protected <TYPE> StructParser<TYPE> customField(String childName, String doc, StructParser<TYPE> valueParser) {
        return new FieldParser<>(childName, false, null, doc, valueParser);
    }

    protected String validateName(String objectType, String parsedName, String defaultName) {
        return validateName(objectType, parsedName, defaultName, true);
    }

    protected String validateName(String objectType, String parsedName, String defaultName, boolean required) {
        if (parsedName != null) return parsedName;
        if (defaultName != null) return defaultName;
        if (!required) return null;
        return parseError(objectType + " name not defined");
    }

    protected <V> V parseError(String message) {
        throw new TopologyException(message);
    }

    public interface Constructor0<RESULT> {
        RESULT construct(ContextTags tags);
    }

    public interface Constructor1<RESULT, A> {
        RESULT construct(A a, ContextTags tags);
    }

    public interface Constructor2<RESULT, A, B> {
        RESULT construct(A a, B b, ContextTags tags);
    }

    public interface Constructor3<RESULT, A, B, C> {
        RESULT construct(A a, B b, C c, ContextTags tags);
    }

    public interface Constructor4<RESULT, A, B, C, D> {
        RESULT construct(A a, B b, C c, D d, ContextTags tags);
    }

    public interface Constructor5<RESULT, A, B, C, D, E> {
        RESULT construct(A a, B b, C c, D d, E e, ContextTags tags);
    }

    public interface Constructor6<RESULT, A, B, C, D, E, F> {
        RESULT construct(A a, B b, C c, D d, E e, F f, ContextTags tags);
    }

    public interface Constructor7<RESULT, A, B, C, D, E, F, G> {
        RESULT construct(A a, B b, C c, D d, E e, F f, G g, ContextTags tags);
    }

    public interface Constructor8<RESULT, A, B, C, D, E, F, G, H> {
        RESULT construct(A a, B b, C c, D d, E e, F f, G g, H h, ContextTags tags);
    }

    public interface Constructor9<RESULT, A, B, C, D, E, F, G, H, I> {
        RESULT construct(A a, B b, C c, D d, E e, F f, G g, H h, I i, ContextTags tags);
    }

    public interface Constructor10<RESULT, A, B, C, D, E, F, G, H, I, J> {
        RESULT construct(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, ContextTags tags);
    }

    public interface Constructor11<RESULT, A, B, C, D, E, F, G, H, I, J, K> {
        RESULT construct(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, ContextTags tags);
    }

    protected <TYPE> StructParser<TYPE> structParser(Class<TYPE> resultClass, String definitionVariant, String doc, Constructor0<TYPE> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(), node -> constructor.construct(node.tags()));
    }

    protected <TYPE, A> StructParser<TYPE> structParser(Class<TYPE> resultClass, String definitionVariant, String doc, StructParser<A> a, Constructor1<TYPE, A> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(a), node -> constructor.construct(a.parse(node), node.tags()));
    }

    protected <TYPE, A, B> StructParser<TYPE> structParser(Class<? extends TYPE> resultClass, String definitionVariant, String doc, StructParser<A> a, StructParser<B> b, Constructor2<TYPE, A, B> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(a, b), node -> constructor.construct(a.parse(node), b.parse(node), node.tags()));
    }

    protected <TYPE, A, B, C> StructParser<TYPE> structParser(Class<? extends TYPE> resultClass, String definitionVariant, String doc, StructParser<A> a, StructParser<B> b, StructParser<C> c, Constructor3<TYPE, A, B, C> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(a, b, c), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), node.tags()));
    }

    protected <TYPE, A, B, C, D> StructParser<TYPE> structParser(Class<? extends TYPE> resultClass, String definitionVariant, String doc, StructParser<A> a, StructParser<B> b, StructParser<C> c, StructParser<D> d, Constructor4<TYPE, A, B, C, D> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(a, b, c, d), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), node.tags()));
    }

    protected <TYPE, A, B, C, D, E> StructParser<TYPE> structParser(Class<? extends TYPE> resultClass, String definitionVariant, String doc, StructParser<A> a, StructParser<B> b, StructParser<C> c, StructParser<D> d, StructParser<E> e, Constructor5<TYPE, A, B, C, D, E> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(a, b, c, d, e), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), node.tags()));
    }

    protected <TYPE, A, B, C, D, E, F> StructParser<TYPE> structParser(Class<? extends TYPE> resultClass, String definitionVariant, String doc, StructParser<A> a, StructParser<B> b, StructParser<C> c, StructParser<D> d, StructParser<E> e, StructParser<F> f, Constructor6<TYPE, A, B, C, D, E, F> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(a, b, c, d, e, f), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node), node.tags()));
    }

    protected <TYPE, A, B, C, D, E, F, G> StructParser<TYPE> structParser(Class<? extends TYPE> resultClass, String definitionVariant, String doc, StructParser<A> a, StructParser<B> b, StructParser<C> c, StructParser<D> d, StructParser<E> e, StructParser<F> f, StructParser<G> g, Constructor7<TYPE, A, B, C, D, E, F, G> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(a, b, c, d, e, f, g), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node), g.parse(node), node.tags()));
    }

    protected <TYPE, A, B, C, D, E, F, G, H> StructParser<TYPE> structParser(Class<? extends TYPE> resultClass, String definitionVariant, String doc, StructParser<A> a, StructParser<B> b, StructParser<C> c, StructParser<D> d, StructParser<E> e, StructParser<F> f, StructParser<G> g, StructParser<H> h, Constructor8<TYPE, A, B, C, D, E, F, G, H> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(a, b, c, d, e, f, g, h), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node), g.parse(node), h.parse(node), node.tags()));
    }

    protected <TYPE, A, B, C, D, E, F, G, H, I> StructParser<TYPE> structParser(Class<? extends TYPE> resultClass, String definitionVariant, String doc, StructParser<A> a, StructParser<B> b, StructParser<C> c, StructParser<D> d, StructParser<E> e, StructParser<F> f, StructParser<G> g, StructParser<H> h, StructParser<I> i, Constructor9<TYPE, A, B, C, D, E, F, G, H, I> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(a, b, c, d, e, f, g, h, i), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node), g.parse(node), h.parse(node), i.parse(node), node.tags()));
    }

    protected <TYPE, A, B, C, D, E, F, G, H, I, J> StructParser<TYPE> structParser(Class<? extends TYPE> resultClass, String definitionVariant, String doc, StructParser<A> a, StructParser<B> b, StructParser<C> c, StructParser<D> d, StructParser<E> e, StructParser<F> f, StructParser<G> g, StructParser<H> h, StructParser<I> i, StructParser<J> j, Constructor10<TYPE, A, B, C, D, E, F, G, H, I, J> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(a, b, c, d, e, f, g, h, i, j), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node), g.parse(node), h.parse(node), i.parse(node), j.parse(node), node.tags()));
    }

    protected <TYPE, A, B, C, D, E, F, G, H, I, J, K> StructParser<TYPE> structParser(Class<? extends TYPE> resultClass, String definitionVariant, String doc, StructParser<A> a, StructParser<B> b, StructParser<C> c, StructParser<D> d, StructParser<E> e, StructParser<F> f, StructParser<G> g, StructParser<H> h, StructParser<I> i, StructParser<J> j, StructParser<K> k, Constructor11<TYPE, A, B, C, D, E, F, G, H, I, J, K> constructor) {
        return new ValueStructParser<>(resultClass.getSimpleName() + definitionVariant, doc, List.of(a, b, c, d, e, f, g, h, i, j, k), node -> constructor.construct(a.parse(node), b.parse(node), c.parse(node), d.parse(node), e.parse(node), f.parse(node), g.parse(node), h.parse(node), i.parse(node), j.parse(node), k.parse(node), node.tags()));
    }
}
