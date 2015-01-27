/*
 * Created by brightSPARK Labs
 * www.brightsparklabs.com
 */

package com.brightsparklabs.asanti.model.schema;

import static com.google.common.base.Preconditions.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * A 'constructed' (SET, SEQUENCE, SET OF, SEQUENCE OF, CHOICE or ENUMERATED)
 * type definition from a within a module specification within an ASN.1 schema.
 *
 * @author brightSPARK Labs
 */
public class AsnSchemaTypeDefinitionConstructed extends AsnSchemaTypeDefinition
{
    // -------------------------------------------------------------------------
    // CLASS VARIABLES
    // -------------------------------------------------------------------------

    /** class logger */
    private static final Logger log = Logger.getLogger(AsnSchemaTypeDefinitionConstructed.class.getName());

    /**
     * built-in types which are considered 'constructed'. Currently: SET,
     * SEQUENCE, SET OF, SEQUENCE OF or CHOICE
     */
    public static final ImmutableSet<AsnBuiltinType> validTypes = ImmutableSet.of(AsnBuiltinType.Set,
            AsnBuiltinType.Sequence,
            AsnBuiltinType.SetOf,
            AsnBuiltinType.SequenceOf,
            AsnBuiltinType.Choice);

    // -------------------------------------------------------------------------
    // INSTANCE VARIABLES
    // -------------------------------------------------------------------------

    /** mapping from raw tag to component type */
    private final ImmutableMap<String, AsnSchemaComponentType> tagsToComponentTypes;

    // -------------------------------------------------------------------------
    // CONSTRUCTION
    // -------------------------------------------------------------------------

    /**
     * Default constructor.
     *
     * @param name
     *            name of the defined type
     *
     * @param type
     *            the underlying ASN.1 type of the defined type
     *
     * @param componentTypes
     *            the component types within this defined type
     *
     * @throws NullPointerException
     *             if {@code name}, {@code type} or {@code componentTypes} are
     *             {@code null}
     *
     * @throws IllegalArgumentException
     *             if {@code name} is blank or {@code type} is not one of the
     *             valid types defined in {@link #validTypes}
     */
    public AsnSchemaTypeDefinitionConstructed(String name, AsnBuiltinType type,
            Iterable<AsnSchemaComponentType> componentTypes)
    {
        super(name, type);
        checkArgument(validTypes.contains(type), "Type must be either SET, SEQUENCE, SET OF, SEQUENCE OF or CHOICE");
        checkNotNull(componentTypes);

        final ImmutableMap.Builder<String, AsnSchemaComponentType> tagsToComponentTypesBuilder = ImmutableMap.builder();

        // next expected tag is used to generate tags for automatic tagging
        // TODO ensure that generating for all missing tags is correct and safe
        int nextExpectedTag = 0;

        for (final AsnSchemaComponentType componentType : componentTypes)
        {
            String tag = componentType.getTag();
            if (Strings.isNullOrEmpty(tag))
            {
                tag = String.valueOf(nextExpectedTag);
                log.log(Level.FINE,
                        "Generated automatic tag [{0}] for {1}",
                        new Object[] { tag, componentType.getTagName() });
                nextExpectedTag++;
            }
            else
            {
                nextExpectedTag = Integer.parseInt(tag) + 1;
            }
            tagsToComponentTypesBuilder.put(tag, componentType);
        }
        tagsToComponentTypes = tagsToComponentTypesBuilder.build();
    }

    // -------------------------------------------------------------------------
    // IMPLEMENTATION: AsnSchemaTypeDefinition
    // -------------------------------------------------------------------------

    @Override
    public String getTagName(String tag)
    {
        final AsnSchemaTag schemaTag = AsnSchemaTag.create(tag);
        if (schemaTag == AsnSchemaTag.NULL)
        {
            log.log(Level.WARNING, "Invalid tag supplied. Expected format: 'tag' or 'tag[index]', received: {0}", tag);
            return "";
        }

        final AsnSchemaComponentType componentType = tagsToComponentTypes.get(schemaTag.getTagNumber());
        return (componentType == null) ? "" : componentType.getTagName() + schemaTag.getTagIndex();
    }

    @Override
    public String getTypeName(String tag)
    {
        final AsnSchemaTag schemaTag = AsnSchemaTag.create(tag);
        if (schemaTag == AsnSchemaTag.NULL)
        {
            log.log(Level.WARNING, "Invalid tag supplied. Expected format: 'tag' or 'tag[index]', received: {0}", tag);
            return "";
        }

        final AsnSchemaComponentType componentType = tagsToComponentTypes.get(schemaTag.getTagNumber());
        return (componentType == null) ? "" : componentType.getTypeName();
    }

    // -------------------------------------------------------------------------
    // INTERNAL CLASS: AsnSchemaTag
    // -------------------------------------------------------------------------

    /**
     * Models a tag in a 'constructed' type definition. A tag conforms to one of
     * the following formats:
     * <ul>
     * <li>tagNumber (e.g. {@code 1}</li>
     * <li>tagNumber[tagIndex] (e.g. {@code 1[0]}</li>
     * </ul>
     */
    private static class AsnSchemaTag
    {
        // ---------------------------------------------------------------------
        // CONSTANTS
        // ---------------------------------------------------------------------

        /** null instance */
        private static final AsnSchemaTag NULL = new AsnSchemaTag("", "");

        /** pattern to check raw tag against */
        private static final Pattern PATTERN_TAG = Pattern.compile("(\\d+)(\\[\\d+\\])?$");

        // ---------------------------------------------------------------------
        // INSTANCE VARIABLES
        // ---------------------------------------------------------------------

        /** the tag number component of the raw tag */
        private final String tagNumber;

        /** the tag index component of the raw tag. Blank if no index component */
        private final String tagIndex;

        // ---------------------------------------------------------------------
        // CONSTRUCTION
        // ---------------------------------------------------------------------

        /**
         * Default constructor. Private, use {@link #create(String)} instead.
         *
         * @param tagNumber
         *            tag number component of the raw tag
         *
         * @param tagIndex
         *            tag index component of the raw tag. Set to {@code null} if
         *            no index component.
         */
        private AsnSchemaTag(String tagNumber, String tagIndex)
        {
            this.tagNumber = tagNumber;
            this.tagIndex = tagIndex == null ? "" : tagIndex;
        }

        /**
         * Creates an instance from the supplied raw tag
         *
         * @param rawTag
         *            raw tag to create instance from
         *
         * @return instance which models the raw tag, or {@value #NULL} if the
         *         raw tag is invalid
         */
        public static AsnSchemaTag create(String rawTag)
        {
            if (rawTag == null) { return NULL; }

            final Matcher matcher = PATTERN_TAG.matcher(rawTag);
            if (matcher.matches())
            {
                return new AsnSchemaTag(matcher.group(1), matcher.group(2));
            }
            else
            {
                return NULL;
            }
        }

        // ---------------------------------------------------------------------
        // PUBLIC METHODS
        // ---------------------------------------------------------------------

        /**
         * Returns the tag number component of the raw tag. For a raw tag
         * {@code "1[0]"} this is {@code "1"}.
         *
         * @return the tag number
         */
        public String getTagNumber()
        {
            return tagNumber;
        }

        /**
         * Returns the tag index component of the raw tag. For a raw tag
         * {@code "1[0]"} this is {@code "[0]"}.
         *
         * @return the tag index or a blank string if no index component
         */
        public String getTagIndex()
        {
            return tagIndex;
        }
    }
}
