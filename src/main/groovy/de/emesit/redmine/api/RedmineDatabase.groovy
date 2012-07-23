package de.emesit.redmine.api

import groovy.sql.Sql

class RedmineDatabase {
    String name
    String jdbcUrl
    String jdbcUsername
    String jdbcPassword
    String jdbcDriver
    
    List<CustomField> customFields
    List<Enumeration> enumerations
    
    boolean apiKeyExists(String apiKey) {
        if (apiKey) {
            Sql sql = Sql.newInstance(jdbcUrl, jdbcUsername, jdbcPassword, jdbcDriver)
            try {
                return sql.firstRow('select count(*) as numberOfRows from tokens where action=? and value=?', ['api', apiKey]).numberOfRows == 1
            } finally {
                if (sql != null) {
                    sql.close()
                }
            }
        }
    }
    
    List<CustomField> getCustomFields(String type, boolean reload = false) {
        List<CustomField> customFields = getCustomFields(reload)
        return customFields.findAll { it.type == type }
    }

    List<CustomField> getCustomFields(boolean reload = false) {
        Sql sql = null
        try {
            if (reload || customFields == null) {
                sql = Sql.newInstance(jdbcUrl, jdbcUsername, jdbcPassword, jdbcDriver)
                customFields = CustomField.queryAllCustomFields(sql)
            }
        } finally {
            if (sql != null) {
                sql.close()
            }
        }
        return customFields
    }
    
    List<CustomField> getEnumerations(String type, boolean reload = false) {
        List<Enumeration> enumerations = getEnumerations(reload)
        return enumerations.findAll { it.type == type }
    }

    List<CustomField> getEnumerations(boolean reload = false) {
        Sql sql = null
        try {
            if (reload || enumerations == null) {
                sql = Sql.newInstance(jdbcUrl, jdbcUsername, jdbcPassword, jdbcDriver)
                enumerations = Enumeration.queryAllEnumerations(sql)
            }
        } finally {
            if (sql != null) {
                sql.close()
            }
        }
        return enumerations
    }
    
    @Override
    String toString() {
        "$name[$jdbcUrl]"
    }
    
}
