package fr.sparkit.accounting.restcontroller;

import fr.sparkit.accounting.dto.DataBaseDto;
import fr.sparkit.accounting.services.utils.DBManagementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounting/database")
public class DBManagementController {

    private DBManagementUtil dbManagementUtil;

    @Autowired
    public DBManagementController(DBManagementUtil dbManagementUtil) {
        this.dbManagementUtil = dbManagementUtil;
    }

    @Transactional
    @PostMapping
    public String createDataBase(@RequestBody DataBaseDto dataBaseDto){
      return dbManagementUtil.createDataBase(dataBaseDto);
    }



}
