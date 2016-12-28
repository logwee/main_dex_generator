/*
 * Copyright (C) 2011 The Android Open Source Project
 *
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
 */

package com.droid.dx.merge;

import com.droid.dex.ProtoId;

import java.util.HashMap;

/**
 * Maps the index offsets from one dex file to those in another. For example, if
 * you have string #5 in the old dex file, its position in the new dex file is
 * {@code strings[5]}.
 */
public final class IndexMap {
    private final com.droid.dex.Dex target;
    public final int[] stringIds;
    public final short[] typeIds;
    public final short[] protoIds;
    public final short[] fieldIds;
    public final short[] methodIds;
    private final HashMap<Integer, Integer> typeListOffsets;
    private final HashMap<Integer, Integer> annotationOffsets;
    private final HashMap<Integer, Integer> annotationSetOffsets;
    private final HashMap<Integer, Integer> annotationSetRefListOffsets;
    private final HashMap<Integer, Integer> annotationDirectoryOffsets;
    private final HashMap<Integer, Integer> staticValuesOffsets;

    public IndexMap(com.droid.dex.Dex target, com.droid.dex.TableOfContents tableOfContents) {
        this.target = target;
        this.stringIds = new int[tableOfContents.stringIds.size];
        this.typeIds = new short[tableOfContents.typeIds.size];
        this.protoIds = new short[tableOfContents.protoIds.size];
        this.fieldIds = new short[tableOfContents.fieldIds.size];
        this.methodIds = new short[tableOfContents.methodIds.size];
        this.typeListOffsets = new HashMap<Integer, Integer>();
        this.annotationOffsets = new HashMap<Integer, Integer>();
        this.annotationSetOffsets = new HashMap<Integer, Integer>();
        this.annotationSetRefListOffsets = new HashMap<Integer, Integer>();
        this.annotationDirectoryOffsets = new HashMap<Integer, Integer>();
        this.staticValuesOffsets = new HashMap<Integer, Integer>();

        /*
         * A type list, annotation set, annotation directory, or static value at
         * offset 0 is always empty. Always map offset 0 to 0.
         */
        this.typeListOffsets.put(0, 0);
        this.annotationSetOffsets.put(0, 0);
        this.annotationDirectoryOffsets.put(0, 0);
        this.staticValuesOffsets.put(0, 0);
    }

    public void putTypeListOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        typeListOffsets.put(oldOffset, newOffset);
    }

    public void putAnnotationOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        annotationOffsets.put(oldOffset, newOffset);
    }

    public void putAnnotationSetOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        annotationSetOffsets.put(oldOffset, newOffset);
    }

    public void putAnnotationSetRefListOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        annotationSetRefListOffsets.put(oldOffset, newOffset);
    }

    public void putAnnotationDirectoryOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        annotationDirectoryOffsets.put(oldOffset, newOffset);
    }

    public void putStaticValuesOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        staticValuesOffsets.put(oldOffset, newOffset);
    }

    public int adjustString(int stringIndex) {
        return stringIndex == com.droid.dex.ClassDef.NO_INDEX ? com.droid.dex.ClassDef.NO_INDEX : stringIds[stringIndex];
    }

    public int adjustType(int typeIndex) {
        return (typeIndex == com.droid.dex.ClassDef.NO_INDEX) ? com.droid.dex.ClassDef.NO_INDEX : (typeIds[typeIndex] & 0xffff);
    }

    public com.droid.dex.TypeList adjustTypeList(com.droid.dex.TypeList typeList) {
        if (typeList == com.droid.dex.TypeList.EMPTY) {
            return typeList;
        }
        short[] types = typeList.getTypes().clone();
        for (int i = 0; i < types.length; i++) {
            types[i] = (short) adjustType(types[i]);
        }
        return new com.droid.dex.TypeList(target, types);
    }

    public int adjustProto(int protoIndex) {
        return protoIds[protoIndex] & 0xffff;
    }

    public int adjustField(int fieldIndex) {
        return fieldIds[fieldIndex] & 0xffff;
    }

    public int adjustMethod(int methodIndex) {
        return methodIds[methodIndex] & 0xffff;
    }

    public int adjustTypeListOffset(int typeListOffset) {
        return typeListOffsets.get(typeListOffset);
    }

    public int adjustAnnotation(int annotationOffset) {
        return annotationOffsets.get(annotationOffset);
    }

    public int adjustAnnotationSet(int annotationSetOffset) {
        return annotationSetOffsets.get(annotationSetOffset);
    }

    public int adjustAnnotationSetRefList(int annotationSetRefListOffset) {
        return annotationSetRefListOffsets.get(annotationSetRefListOffset);
    }

    public int adjustAnnotationDirectory(int annotationDirectoryOffset) {
        return annotationDirectoryOffsets.get(annotationDirectoryOffset);
    }

    public int adjustStaticValues(int staticValuesOffset) {
        return staticValuesOffsets.get(staticValuesOffset);
    }

    public com.droid.dex.MethodId adjust(com.droid.dex.MethodId methodId) {
        return new com.droid.dex.MethodId(target,
                adjustType(methodId.getDeclaringClassIndex()),
                adjustProto(methodId.getProtoIndex()),
                adjustString(methodId.getNameIndex()));
    }

    public com.droid.dex.FieldId adjust(com.droid.dex.FieldId fieldId) {
        return new com.droid.dex.FieldId(target,
                adjustType(fieldId.getDeclaringClassIndex()),
                adjustType(fieldId.getTypeIndex()),
                adjustString(fieldId.getNameIndex()));

    }

    public ProtoId adjust(ProtoId protoId) {
        return new ProtoId(target,
                adjustString(protoId.getShortyIndex()),
                adjustType(protoId.getReturnTypeIndex()),
                adjustTypeListOffset(protoId.getParametersOffset()));
    }

    public com.droid.dex.ClassDef adjust(com.droid.dex.ClassDef classDef) {
        return new com.droid.dex.ClassDef(target, classDef.getOffset(), adjustType(classDef.getTypeIndex()),
                classDef.getAccessFlags(), adjustType(classDef.getSupertypeIndex()),
                adjustTypeListOffset(classDef.getInterfacesOffset()), classDef.getSourceFileIndex(),
                classDef.getAnnotationsOffset(), classDef.getClassDataOffset(),
                classDef.getStaticValuesOffset());
    }

    public SortableType adjust(SortableType sortableType) {
        return new SortableType(sortableType.getDex(),
                sortableType.getIndexMap(), adjust(sortableType.getClassDef()));
    }

    public com.droid.dex.EncodedValue adjustEncodedValue(com.droid.dex.EncodedValue encodedValue) {
        com.droid.dx.util.ByteArrayAnnotatedOutput out = new com.droid.dx.util.ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transform(new com.droid.dex.EncodedValueReader(encodedValue));
        return new com.droid.dex.EncodedValue(out.toByteArray());
    }

    public com.droid.dex.EncodedValue adjustEncodedArray(com.droid.dex.EncodedValue encodedArray) {
        com.droid.dx.util.ByteArrayAnnotatedOutput out = new com.droid.dx.util.ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transformArray(
                new com.droid.dex.EncodedValueReader(encodedArray, com.droid.dex.EncodedValueReader.ENCODED_ARRAY));
        return new com.droid.dex.EncodedValue(out.toByteArray());
    }

    public com.droid.dex.Annotation adjust(com.droid.dex.Annotation annotation) {
        com.droid.dx.util.ByteArrayAnnotatedOutput out = new com.droid.dx.util.ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transformAnnotation(
                annotation.getReader());
        return new com.droid.dex.Annotation(target, annotation.getVisibility(),
                new com.droid.dex.EncodedValue(out.toByteArray()));
    }

    /**
     * Adjust an encoded value or array.
     */
    private final class EncodedValueTransformer {
        private final com.droid.dex.util.ByteOutput out;

        public EncodedValueTransformer(com.droid.dex.util.ByteOutput out) {
            this.out = out;
        }

        public void transform(com.droid.dex.EncodedValueReader reader) {
            // TODO: extract this into a helper class, EncodedValueWriter
            switch (reader.peek()) {
            case com.droid.dex.EncodedValueReader.ENCODED_BYTE:
                com.droid.dex.EncodedValueCodec.writeSignedIntegralValue(out, com.droid.dex.EncodedValueReader.ENCODED_BYTE, reader.readByte());
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_SHORT:
                com.droid.dex.EncodedValueCodec.writeSignedIntegralValue(out, com.droid.dex.EncodedValueReader.ENCODED_SHORT, reader.readShort());
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_INT:
                com.droid.dex.EncodedValueCodec.writeSignedIntegralValue(out, com.droid.dex.EncodedValueReader.ENCODED_INT, reader.readInt());
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_LONG:
                com.droid.dex.EncodedValueCodec.writeSignedIntegralValue(out, com.droid.dex.EncodedValueReader.ENCODED_LONG, reader.readLong());
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_CHAR:
                com.droid.dex.EncodedValueCodec.writeUnsignedIntegralValue(out, com.droid.dex.EncodedValueReader.ENCODED_CHAR, reader.readChar());
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_FLOAT:
                // Shift value left 32 so that right-zero-extension works.
                long longBits = ((long) Float.floatToIntBits(reader.readFloat())) << 32;
                com.droid.dex.EncodedValueCodec.writeRightZeroExtendedValue(out, com.droid.dex.EncodedValueReader.ENCODED_FLOAT, longBits);
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_DOUBLE:
                com.droid.dex.EncodedValueCodec.writeRightZeroExtendedValue(
                        out, com.droid.dex.EncodedValueReader.ENCODED_DOUBLE, Double.doubleToLongBits(reader.readDouble()));
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_STRING:
                com.droid.dex.EncodedValueCodec.writeUnsignedIntegralValue(
                        out, com.droid.dex.EncodedValueReader.ENCODED_STRING, adjustString(reader.readString()));
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_TYPE:
                com.droid.dex.EncodedValueCodec.writeUnsignedIntegralValue(
                        out, com.droid.dex.EncodedValueReader.ENCODED_TYPE, adjustType(reader.readType()));
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_FIELD:
                com.droid.dex.EncodedValueCodec.writeUnsignedIntegralValue(
                        out, com.droid.dex.EncodedValueReader.ENCODED_FIELD, adjustField(reader.readField()));
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_ENUM:
                com.droid.dex.EncodedValueCodec.writeUnsignedIntegralValue(
                        out, com.droid.dex.EncodedValueReader.ENCODED_ENUM, adjustField(reader.readEnum()));
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_METHOD:
                com.droid.dex.EncodedValueCodec.writeUnsignedIntegralValue(
                        out, com.droid.dex.EncodedValueReader.ENCODED_METHOD, adjustMethod(reader.readMethod()));
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_ARRAY:
                writeTypeAndArg(com.droid.dex.EncodedValueReader.ENCODED_ARRAY, 0);
                transformArray(reader);
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_ANNOTATION:
                writeTypeAndArg(com.droid.dex.EncodedValueReader.ENCODED_ANNOTATION, 0);
                transformAnnotation(reader);
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_NULL:
                reader.readNull();
                writeTypeAndArg(com.droid.dex.EncodedValueReader.ENCODED_NULL, 0);
                break;
            case com.droid.dex.EncodedValueReader.ENCODED_BOOLEAN:
                boolean value = reader.readBoolean();
                writeTypeAndArg(com.droid.dex.EncodedValueReader.ENCODED_BOOLEAN, value ? 1 : 0);
                break;
            default:
                throw new com.droid.dex.DexException("Unexpected type: " + Integer.toHexString(reader.peek()));
            }
        }

        private void transformAnnotation(com.droid.dex.EncodedValueReader reader) {
            int fieldCount = reader.readAnnotation();
            com.droid.dex.Leb128.writeUnsignedLeb128(out, adjustType(reader.getAnnotationType()));
            com.droid.dex.Leb128.writeUnsignedLeb128(out, fieldCount);
            for (int i = 0; i < fieldCount; i++) {
                com.droid.dex.Leb128.writeUnsignedLeb128(out, adjustString(reader.readAnnotationName()));
                transform(reader);
            }
        }

        private void transformArray(com.droid.dex.EncodedValueReader reader) {
            int size = reader.readArray();
            com.droid.dex.Leb128.writeUnsignedLeb128(out, size);
            for (int i = 0; i < size; i++) {
                transform(reader);
            }
        }

        private void writeTypeAndArg(int type, int arg) {
            out.writeByte((arg << 5) | type);
        }
    }
}
