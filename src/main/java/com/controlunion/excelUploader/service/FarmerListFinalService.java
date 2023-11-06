package com.controlunion.excelUploader.service;

import com.controlunion.excelUploader.model.FarmerListFinal;
import com.controlunion.excelUploader.repository.FarmerListFinalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class FarmerListFinalService {

    private final FarmerListFinalRepository farmerListFinalRepository;
    private final Random random = new Random();

//    public FarmerListFinal isExistingFarmer(String farmerCodeEUJAS, int proId){
//
//        return getFarmerByCodeAndProId(farmerCodeEUJAS, proId);
//    }
//
//    public int checkIsNewFarmer(String farmerCode, int proId) {
//        FarmerListFinal farmerListFinal = getFarmerByCodeAndProId(farmerCode, proId);
//        if (farmerListFinal == null) {
//            return createCuid();
//        } else {
//            return farmerListFinal.getCufarmerID();
//        }
//    }

    public int createCuid() {
        int min = 100_000_000;
        int max = 999_999_999;

        int randomNineDigits = random.nextInt(max - min + 1) + min;
        int cuid = randomNineDigits;
        if (checkCUIDExist(cuid)){
            return createCuid();
        }
        return cuid;
    }

    private boolean checkCUIDExist(int cuid){
        return farmerListFinalRepository.existsByCufarmerID(cuid);
    }

    public List<FarmerListFinal> getFarmerByCodeAndProIdAndAuditId(String farmerCodeEUJAS,
                                                                   int proId,
                                                                   int auditId) {
        log.info(getClass().getName()+".checkCUIDExist: check farmer already exist "+farmerCodeEUJAS+" proID : "+proId);
        try {
            List<FarmerListFinal> farmerListFinal = farmerListFinalRepository
                    .findAllByFarCodeEUJASAndProID(farmerCodeEUJAS, proId).get();
            log.info(getClass().getName()+".checkCUIDExist: farCodeEUJAS : "+farmerCodeEUJAS+" cuid : "+farmerListFinal.get(0).getCufarmerID());
            return farmerListFinal;
        }catch (Exception e){
            e.getStackTrace();
            log.info(getClass().getName()+".checkCUIDExist: farCodeEUJAS : "+farmerCodeEUJAS+" not exist ");
            return null;
        }
    }

//    public int getFarmerCUIDByFarmerCode(String farmerCOdeEUJAS, int proId){
//        log.info(getClass().getName()+".getFarmerCUIDByFarmerCode: getting cuid for "+farmerCOdeEUJAS);
//        try {
//            return getFarmerByCodeAndProId(farmerCOdeEUJAS, proId).getCufarmerID();
//        }catch (Exception e){
//            log.error(getClass().getName()+".getFarmerCUIDByFarmerCode: getting cuid for "+farmerCOdeEUJAS+" : "+e.getMessage());
//            e.printStackTrace();
//            return 0;
//        }
//    }


    public List<FarmerListFinal> getFarmerListFinalByProjectIdAndCuid(int cufarmerId, int prodId) {
        return farmerListFinalRepository.findAllByCufarmerIDAndProID(cufarmerId, prodId).orElse(null);
    }
}
