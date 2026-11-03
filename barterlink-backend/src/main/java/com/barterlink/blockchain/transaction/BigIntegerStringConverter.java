package com.barterlink.blockchain.transaction;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigInteger;

@Converter
public class BigIntegerStringConverter
        implements AttributeConverter<BigInteger, String> {

    @Override
    public String convertToDatabaseColumn(BigInteger attribute) {

        if (attribute == null) {
            return null;
        }

        return attribute.toString();
    }

    @Override
    public BigInteger convertToEntityAttribute(String dbData) {

        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        return new BigInteger(dbData);
    }
}