package com.axelor.apps.budget.service;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.budget.db.Budget;
import com.axelor.apps.budget.db.BudgetLevel;
import com.axelor.apps.budget.db.GlobalBudget;
import com.axelor.apps.budget.db.repo.GlobalBudgetRepository;
import com.axelor.common.ObjectUtils;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.util.List;

public class GlobalBudgetServiceImpl implements GlobalBudgetService {

  protected BudgetLevelService budgetLevelService;
  protected GlobalBudgetRepository globalBudgetRepository;

  @Inject
  public GlobalBudgetServiceImpl(
      BudgetLevelService budgetLevelService, GlobalBudgetRepository globalBudgetRepository) {
    this.budgetLevelService = budgetLevelService;
    this.globalBudgetRepository = globalBudgetRepository;
  }

  @Override
  public void validateDates(GlobalBudget globalBudget) throws AxelorException {
    if (!ObjectUtils.isEmpty(globalBudget.getBudgetLevelList())) {
      for (BudgetLevel budgetLevelChild : globalBudget.getBudgetLevelList()) {
        budgetLevelService.validateDates(budgetLevelChild);
      }
    }
  }

  @Override
  @Transactional(rollbackOn = {RuntimeException.class})
  public void computeBudgetLevelTotals(Budget budget) {

    budgetLevelService.computeBudgetLevelTotals(budget);

    GlobalBudget globalBudget = budget.getGlobalBudget();
    if (globalBudget != null) {
      computeTotals(globalBudget);
      globalBudgetRepository.save(globalBudget);
    }
  }

  @Override
  public void computeTotals(GlobalBudget globalBudget) {
    List<BudgetLevel> budgetLevelList = globalBudget.getBudgetLevelList();
    BigDecimal totalAmountExpected = BigDecimal.ZERO;
    BigDecimal totalAmountCommitted = BigDecimal.ZERO;
    BigDecimal totalAmountRealized = BigDecimal.ZERO;
    BigDecimal realizedWithPo = BigDecimal.ZERO;
    BigDecimal realizedWithNoPo = BigDecimal.ZERO;
    BigDecimal totalAmountPaid = BigDecimal.ZERO;
    BigDecimal totalFirmGap = BigDecimal.ZERO;
    BigDecimal simulatedAmount = BigDecimal.ZERO;
    if (budgetLevelList != null) {
      for (BudgetLevel budgetLevelObj : budgetLevelList) {
        totalAmountExpected = totalAmountExpected.add(budgetLevelObj.getTotalAmountExpected());
        totalAmountCommitted = totalAmountCommitted.add(budgetLevelObj.getTotalAmountCommitted());
        totalAmountPaid = totalAmountPaid.add(budgetLevelObj.getTotalAmountPaid());
        totalAmountRealized = totalAmountRealized.add(budgetLevelObj.getTotalAmountRealized());
        realizedWithPo = realizedWithPo.add(budgetLevelObj.getRealizedWithPo());
        realizedWithNoPo = realizedWithNoPo.add(budgetLevelObj.getRealizedWithNoPo());
        totalFirmGap = totalFirmGap.add(budgetLevelObj.getTotalFirmGap());
        simulatedAmount = simulatedAmount.add(budgetLevelObj.getSimulatedAmount());
      }
    }
    globalBudget.setTotalAmountExpected(totalAmountExpected);
    globalBudget.setTotalAmountCommitted(totalAmountCommitted);
    globalBudget.setTotalAmountPaid(totalAmountPaid);
    globalBudget.setTotalAmountRealized(totalAmountRealized);
    globalBudget.setRealizedWithNoPo(realizedWithNoPo);
    globalBudget.setRealizedWithPo(realizedWithPo);
    globalBudget.setTotalAmountAvailable(
        (totalAmountExpected.subtract(realizedWithPo).subtract(realizedWithNoPo))
                    .compareTo(BigDecimal.ZERO)
                > 0
            ? totalAmountExpected.subtract(realizedWithPo).subtract(realizedWithNoPo)
            : BigDecimal.ZERO);
    globalBudget.setTotalFirmGap(totalFirmGap);
    globalBudget.setSimulatedAmount(simulatedAmount);
    globalBudget.setAvailableAmountWithSimulated(
        (globalBudget.getTotalAmountAvailable().subtract(simulatedAmount))
                    .compareTo(BigDecimal.ZERO)
                > 0
            ? (globalBudget.getTotalAmountAvailable().subtract(simulatedAmount))
            : BigDecimal.ZERO);
  }
}
