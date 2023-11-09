package com.controlunion.excelUploader.util;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CustomIdentifierGenerator implements IdentifierGenerator {
    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object o) throws HibernateException {
        Connection connection = session.connection();

        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT MAX(listid) FROM farmerlist");

            int lastId = 0; // Initialize with a default value

            if (rs.next()) {
                lastId = rs.getInt(1);
            }

            int nextId = lastId + 1;

            return nextId;
        } catch (Exception e) {
            // Handle any exceptions that may occur during the ID generation process
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean supportsJdbcBatchInserts() {
        return IdentifierGenerator.super.supportsJdbcBatchInserts();
    }
}
