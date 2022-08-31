package fr.sparkit.accounting.services;

import java.util.List;

import fr.sparkit.accounting.dto.ChartAccountsDto;
import fr.sparkit.accounting.dto.ChartAccountsToBalancedDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.entities.ChartAccounts;

public interface IChartAccountsService extends IGenericService<ChartAccounts, Long> {

    List<ChartAccountsDto> findAllCharts();

    List<ChartAccountsDto> buildAllTree();

    ChartAccountsDto findById(Long id);

    List<ChartAccountsDto> findSubTreeByLabelOrCode(String value);

    List<ChartAccountsDto> buildChildTree(Long parentId);

    boolean deleteSubTree(Long id);

    ChartAccounts update(ChartAccountsDto accountingPlanDto);

    ChartAccounts save(ChartAccountsDto accountingPlanDto);

    ChartAccountsDto findByCode(Integer code);

    ChartAccountsDto findByCodeIteration(Integer code);

    List<ChartAccountsDto> findByCodes(int code);

    void balanceChartAccounts(List<Long> chartAccounts);

    ChartAccountsToBalancedDto getChartAccountsToBalanced();

    List<Integer> getChartAccountsCodeToBalanced();

    boolean isChartAccountUsed(Long idChart);

    FileUploadDto loadChartAccountsExcelData(FileUploadDto fileUploadDto);

    byte[] exportChartAccountsAsExcelFile();

    byte[] exportChartAccountsExcelModel();

    byte[] getChartAccountsExcelFile(String fileName);

    void deleteChartAccountsExcelFile(String fileName);

    List<ChartAccounts> findChartAccountByLength(int length, int customerCode, int supplierCode);

    List<ChartAccounts> findChartAccountTierByLength(int length, int tierCode);
}
