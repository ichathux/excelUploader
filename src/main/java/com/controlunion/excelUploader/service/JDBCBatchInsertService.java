package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.FarmerList;
import com.controlunion.excelUploader.repository.FarmerlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.awt.print.Book;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JDBCBatchInsertService {

    private final FarmerlistRepository farmerlistRepository;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;
    long startTime;
    long endTime;

    public void insertAsBatchGroup(Iterable<FarmerList> farmerLists){
        startTime = System.currentTimeMillis();
        ArrayList<FarmerList> farmerLists1 = new ArrayList<>();
        farmerLists.forEach(farmerLists1::add);


        int total = farmerLists1.size();
        farmerlistRepository.saveAll(farmerLists1);
//        for (int i = 0; i < total; i += batchSize) {
//            if( i+ batchSize > total){
//                List<FarmerList> list = farmerLists1.subList(i, total);
//                farmerlistRepository.saveAll(list);
//                break;
//            }
//            List<FarmerList> list = farmerLists1.subList(i, i + batchSize);
//            farmerlistRepository.saveAll(list);
//        }
        endTime = System.currentTimeMillis();
        log.info(total+" files batch inert finished "+(endTime-startTime)+" ms");

    }

//    private final EntityManager entityManager;
//
//    @Transactional
//    public void batchInsert(int proId, int auditId, ArrayList<FarmerList> entities) {
//        deleteIfExist(proId, auditId);
//        entityManager.unwrap(Session.class).doWork(connection -> {
//            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `farmerlist` (`listid`, `cufarmerID`, `proID`, `auditID`, `unitNoEUJAS`, `farCodeEUJAS`, `unitNoNOP`, `farCodeNOP`, `farmerName`, `totalArea`, `city`, `gps`, `address`, `dateCert`, `aplyRetrospe`, `certification`, `fertilizer`, `ferUseDate`, `dateConfersion`, `dateorganic`, `inspected`, `inspectedDate`, `note`, `user`, `sysTimeStamp`, `eujas_field`, `eujas_harvest`, `usda_field`, `usda_harvest`, `auditorNote`, `isNew`, `isChange`, `farmName`, `plotCode`, `chng_farmdata`, `chng_cropdata`) " +
//                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
//                for (FarmerList entity : entities) {
//                    preparedStatement.setInt(1, entity.getListid());
//                    preparedStatement.setLong(2, entity.getCufarmerID());
//                    preparedStatement.setInt(3, entity.getProID());
//                    preparedStatement.setInt(4, entity.getAuditID());
//                    preparedStatement.setString(5, entity.getUnitNoEUJAS());
//                    preparedStatement.setString(6, entity.getFarCodeEUJAS());
//                    preparedStatement.setString(7, entity.getUnitNoNOP());
//                    preparedStatement.setString(8, entity.getFarCodeNOP());
//                    preparedStatement.setString(9, entity.getFarmerName());
//                    preparedStatement.setDouble(10, entity.getTotalArea());
//                    preparedStatement.setString(11, entity.getCity());
//                    preparedStatement.setString(12, entity.getGps());
//                    preparedStatement.setString(13, entity.getAddress()); // Replace with the actual city value
//                    preparedStatement.setDate(14, entity.getDateCert()); // Replace with the actual GPS value
//                    preparedStatement.setInt(15, entity.getAplyRetrospe()); // Replace with the actual address value
//                    preparedStatement.setString(16, entity.getCertification()); // Replace with the actual dateCert value
//                    preparedStatement.setString(17, entity.getFertilizer());
//                    preparedStatement.setString(18, entity.getFerUseDate()); // Replace with the actual aplyRetrospe value
//                    preparedStatement.setDate(19, entity.getDateConfersion()); // Replace with the actual certification value
//                    preparedStatement.setDate(20, entity.getDateorganic()); // Replace with the actual fertilizer value
//                    preparedStatement.setInt(21, entity.getInspected()); // Replace with the actual ferUseDate value
//                    preparedStatement.setDate(22, entity.getInspectedDate()); // Replace with the actual dateConfersion value
//                    preparedStatement.setString(23, entity.getNote()); // Replace with the actual dateorganic value
//                    preparedStatement.setString(24, entity.getUser()); // Replace with the actual dateorganic_nop value
//                    preparedStatement.setDate(25, entity.getSysTimeStamp());
//                    preparedStatement.setString(26, entity.getEujas_field()); // Replace with the actual inspectedDate value
//                    preparedStatement.setString(27, entity.getEujas_harvest()); // Replace with the actual note value
//                    preparedStatement.setString(28, entity.getUsda_field()); // Replace with the actual user value
//                    preparedStatement.setString(29, entity.getUsda_harvest()); // Replace with the actual sysTimeStamp value
//                    preparedStatement.setString(30, entity.getAuditorNote()); // Replace with the actual eujas_field value
//                    preparedStatement.setInt(31, entity.getIsNew()); // Replace with the actual eujas_harvest value
//                    preparedStatement.setInt(32, entity.getIsChange()); // Replace with the actual usda_field value
//                    preparedStatement.setString(33, entity.getFarmName()); // Replace with the actual usda_harvest value
//                    preparedStatement.setString(34, entity.getPlotCode()); // Replace with the actual auditorNote value
//                    preparedStatement.setString(35, entity.getChngFarmdata());
//                    preparedStatement.setString(36, entity.getChngCropdata());
//
//                    preparedStatement.addBatch();
//                }
//                preparedStatement.executeBatch();
//            }
//        });
//    }
//
//    void deleteIfExist(int proId, int auditId){
//        entityManager.unwrap(Session.class).doWork(connection -> {
//            try (PreparedStatement preparedStatement = connection.prepareStatement( "DELETE FROM farmerlist WHERE auditID = ? AND proID = ?;")) {
//                preparedStatement.setInt(1, auditId);
//                preparedStatement.setInt(2, proId);
//                preparedStatement.execute();
//            }
//        });
//    }

}
