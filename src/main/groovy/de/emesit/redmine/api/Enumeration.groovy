package de.emesit.redmine.api

import java.io.StringWriter;
import java.util.List;

import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.xml.MarkupBuilder;


class Enumeration {
    /*
    Currently (redmine 1.4.4) the ENUMERATIONS table is defined like this:
    +------------+--------------+------+-----+---------+----------------+
    | Field      | Type         | Null | Key | Default | Extra          |
    +------------+--------------+------+-----+---------+----------------+
    | id         | int(11)      | NO   | PRI | NULL    | auto_increment |
    | name       | varchar(30)  | NO   |     |         |                |
    | position   | int(11)      | YES  |     | 1       |                |
    | is_default | tinyint(1)   | NO   |     | 0       |                |
    | type       | varchar(255) | YES  |     | NULL    |                |
    | active     | tinyint(1)   | NO   |     | 1       |                |
    | project_id | int(11)      | YES  | MUL | NULL    |                |
    | parent_id  | int(11)      | YES  |     | NULL    |                |
    +------------+--------------+------+-----+---------+----------------+
     */
    
    Integer id
    String  name
    String  type      //NULL POSSIBLE
    boolean isDefault
    boolean active
    Integer projectId //NULL POSSIBLE
    Integer parentId  //NULL POSSIBLE
    Integer position  //NULL POSSIBLE
    
    static Enumeration convertRow(def nextRow) {
        return new Enumeration(
            id            :nextRow.id,
            name          :nextRow.name,
            position      :nextRow.position,
            isDefault     :nextRow.is_default,
            type          :nextRow.type,
            active        :nextRow.active,
            projectId     :nextRow.project_id,
            parentId      :nextRow.parent_id
        )
    }
    
    static List<Enumeration> queryEnumerations(Sql sql, String type) {
        List<Enumeration> enumerations = []
        
        sql.eachRow('select * from enumerations where type=? order by position', [type]) { nextRow ->
            enumerations << convertRow(nextRow)
        }
        
        return enumerations
    }
    
    static List<Enumeration> queryAllEnumerations(Sql sql) {
        List<Enumeration> enumerations = []
        
        sql.eachRow('select * from enumerations order by type, position') { nextRow ->
            enumerations << convertRow(nextRow)
        }
        
        return enumerations
    }
    
    static String toXml(List<Enumeration> enumerations, StringWriter writer = null, MarkupBuilder builder = null) {
        if (builder == null) {
            writer = new StringWriter()
            builder = new MarkupBuilder(writer)
        }
        builder.enumerations(type:'array') {
            for (Enumeration nextEnumeration in enumerations) {
                nextEnumeration.toXml(writer, builder)
            }
        }
        writer.toString()
    }
    
    String toXml(StringWriter writer = null, MarkupBuilder builder = null) {
        if (builder == null) {
            writer = new StringWriter()
            builder = new MarkupBuilder(writer)
        }
        
        builder.enumeration(id:id, name:name) {
            'type'         ("${type}")
            'is_default'   ("${isDefault}")
            'active'       ("${active}")
            if (projectId!=null) {
                'project_id' ("${projectId}")
            }
            if (parentId!=null) {
                'parent_id' ("${parentId}")
            }
        }
        
        writer.toString()
    }
}
