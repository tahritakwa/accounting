---StandardReportLines
---CFA
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Dotations aux amortissements et provisions' WHERE [SRL_REPORT_TYPE]=N'CFA' AND [SRL_LINE_INDEX]=N'(2)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Ajustement provenant de l''exploitation' WHERE [SRL_REPORT_TYPE]=N'CFA' AND [SRL_LINE_INDEX]=N'(8)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Ecaissements liés à la cession d''immobilisations' WHERE [SRL_REPORT_TYPE]=N'CFA' AND [SRL_LINE_INDEX]=N'(10)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Ecaissements liés à la cession d''Immobilisations Financières' WHERE [SRL_REPORT_TYPE]=N'CFA' AND [SRL_LINE_INDEX]=N'(12)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Encaissements Provenant des Subventions d''Investissement' WHERE [SRL_REPORT_TYPE]=N'CFA' AND [SRL_LINE_INDEX]=N'(13)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Flux d''investissement' WHERE [SRL_REPORT_TYPE]=N'CFA' AND [SRL_LINE_INDEX]=N'(14)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Ecaissements des crédits' WHERE [SRL_REPORT_TYPE]=N'CFA' AND [SRL_LINE_INDEX]=N'(16)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Remboursements des crédits' WHERE [SRL_REPORT_TYPE]=N'CFA' AND [SRL_LINE_INDEX]=N'(17)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Dividendes payés' WHERE [SRL_REPORT_TYPE]=N'CFA' AND [SRL_LINE_INDEX]=N'(18)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Variation début d''exercice' WHERE [SRL_REPORT_TYPE]=N'CFA' AND [SRL_LINE_INDEX]=N'(20)';
---CF
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Encaissements Reçus des clients' WHERE [SRL_REPORT_TYPE]=N'CF' AND [SRL_LINE_INDEX]=N'(1)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Impôts sur les bénéfices payés' WHERE [SRL_REPORT_TYPE]=N'CF' AND [SRL_LINE_INDEX]=N'(4)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Flux de trésorerie provenant de (affectés à) l''exploitation' WHERE [SRL_REPORT_TYPE]=N'CF' AND [SRL_LINE_INDEX]=N'(5)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Décaissements provenant de l''acquisition d’immobilisations corporelles et incorporelles' WHERE [SRL_REPORT_TYPE]=N'CF' AND [SRL_LINE_INDEX]=N'(6)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Encaissements provenant de la cession d’immobilisations corporelles et incorporelles' WHERE [SRL_REPORT_TYPE]=N'CF' AND [SRL_LINE_INDEX]=N'(7)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Décaissements provenant de l''acquisition d’immobilisations financières' WHERE [SRL_REPORT_TYPE]=N'CF' AND [SRL_LINE_INDEX]=N'(8)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Encaissements provenant de la cession  d’immobilisation financière' WHERE [SRL_REPORT_TYPE]=N'CF' AND [SRL_LINE_INDEX]=N'(9)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Encaissements suite à l''émission d''actions' WHERE [SRL_REPORT_TYPE]=N'CF' AND [SRL_LINE_INDEX]=N'(11)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Dividendes et autres distributions' WHERE [SRL_REPORT_TYPE]=N'CF' AND [SRL_LINE_INDEX]=N'(12)';
UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_LABEL] = N'Encaissements d''emprunts' WHERE [SRL_REPORT_TYPE]=N'CF' AND [SRL_LINE_INDEX]=N'(13)';

---ReportLines
---CFA
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Dotations aux amortissements et provisions' WHERE [RL_REPORT_TYPE]=N'CFA' AND [RL_LINE_INDEX]=N'(2)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Ajustement provenant de l''exploitation' WHERE [RL_REPORT_TYPE]=N'CFA' AND [RL_LINE_INDEX]=N'(8)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Ecaissements liés à la cession d''Immobilisations' WHERE [RL_REPORT_TYPE]=N'CFA' AND [RL_LINE_INDEX]=N'(10)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Ecaissements liés à la cession d''Immobilisations Financières' WHERE [RL_REPORT_TYPE]=N'CFA' AND [RL_LINE_INDEX]=N'(12)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Encaissements Provenant des Subventions d''Investissement' WHERE [RL_REPORT_TYPE]=N'CFA' AND [RL_LINE_INDEX]=N'(13)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Flux d''investissement' WHERE [RL_REPORT_TYPE]=N'CFA' AND [RL_LINE_INDEX]=N'(14)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Ecaissements des crédits' WHERE [RL_REPORT_TYPE]=N'CFA' AND [RL_LINE_INDEX]=N'(16)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Remboursements des crédits' WHERE [RL_REPORT_TYPE]=N'CFA' AND [RL_LINE_INDEX]=N'(17)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Dividendes payés' WHERE [RL_REPORT_TYPE]=N'CFA' AND [RL_LINE_INDEX]=N'(18)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Variation début d''exercice' WHERE [RL_REPORT_TYPE]=N'CFA' AND [RL_LINE_INDEX]=N'(20)';
---CF
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Encaissements Reçus des clients' WHERE [RL_REPORT_TYPE]=N'CF' AND [RL_LINE_INDEX]=N'(1)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Impôts sur les bénéfices payés' WHERE [RL_REPORT_TYPE]=N'CF' AND [RL_LINE_INDEX]=N'(4)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Flux de trésorerie provenant de (affectés à) l''exploitation' WHERE [RL_REPORT_TYPE]=N'CF' AND [RL_LINE_INDEX]=N'(5)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Décaissements provenant de l''acquisition d’immobilisations corporelles et incorporelles' WHERE [RL_REPORT_TYPE]=N'CF' AND [RL_LINE_INDEX]=N'(6)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Encaissements provenant de la cession d’immobilisations corporelles et incorporelles' WHERE [RL_REPORT_TYPE]=N'CF' AND [RL_LINE_INDEX]=N'(7)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Décaissements provenant de l''acquisition d’immobilisations financières' WHERE [RL_REPORT_TYPE]=N'CF' AND [RL_LINE_INDEX]=N'(8)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Encaissements provenant de la cession  d’immobilisation financière' WHERE [RL_REPORT_TYPE]=N'CF' AND [RL_LINE_INDEX]=N'(9)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Encaissements suite à l''émission d''actions' WHERE [RL_REPORT_TYPE]=N'CF' AND [RL_LINE_INDEX]=N'(11)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Dividendes et autres distributions' WHERE [RL_REPORT_TYPE]=N'CF' AND [RL_LINE_INDEX]=N'(12)';
UPDATE [T_REPORT_LINE]
SET [RL_LABEL] = N'Encaissements d''emprunts' WHERE [RL_REPORT_TYPE]=N'CF' AND [RL_LINE_INDEX]=N'(13)';