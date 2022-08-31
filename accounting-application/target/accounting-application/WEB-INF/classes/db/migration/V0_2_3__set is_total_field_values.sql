---SET IS_TOTAL to true in corresponding StandardReportLines


UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_IS_TOTAL] = 1
WHERE [SRL_LINE_INDEX] IN (N'(4)', N'(23)', N'(24)', N'(25)', N'(31)', N'(33)', N'(36)')
  AND [SRL_REPORT_TYPE] = N'SOI';

UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_IS_TOTAL] = 1
WHERE [SRL_LINE_INDEX] IN
      (N'(4)', N'(10)', N'(15)', N'(16)', N'(18)', N'(21)', N'(24)', N'(29)', N'(30)', N'(31)', N'(36)', N'(38)',
       N'(42)', N'(46)', N'(47)', N'(48)')
  AND [SRL_REPORT_TYPE] = N'BS';

UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_IS_TOTAL] = 1
WHERE [SRL_LINE_INDEX] IN (N'(8)', N'(14)', N'(19)', N'(22)')
  AND [SRL_REPORT_TYPE] = N'CFA';

UPDATE [T_STANDARD_REPORT_LINE]
SET [SRL_IS_TOTAL] = 1
WHERE [SRL_LINE_INDEX] IN (N'(5)', N'(10)', N'(15)', N'(17)', N'(20)')
  AND [SRL_REPORT_TYPE] = N'CF';


---SET IS_TOTAL to true in corresponding ReportLines


UPDATE [T_REPORT_LINE]
SET [RL_IS_TOTAL] = 1
WHERE [RL_LINE_INDEX] IN (N'(4)', N'(23)', N'(24)', N'(25)', N'(31)', N'(33)', N'(36)')
  AND [RL_REPORT_TYPE] = N'SOI';

UPDATE [T_REPORT_LINE]
SET [RL_IS_TOTAL] = 1
WHERE [RL_LINE_INDEX] IN
      (N'(4)', N'(10)', N'(15)', N'(16)', N'(18)', N'(21)', N'(24)', N'(29)', N'(30)', N'(31)', N'(36)', N'(38)',
       N'(42)', N'(46)', N'(47)', N'(48)')
  AND [RL_REPORT_TYPE] = N'BS';

UPDATE [T_REPORT_LINE]
SET [RL_IS_TOTAL] = 1
WHERE [RL_LINE_INDEX] IN (N'(8)', N'(14)', N'(19)', N'(22)')
  AND [RL_REPORT_TYPE] = N'CFA';

UPDATE [T_REPORT_LINE]
SET [RL_IS_TOTAL] = 1
WHERE [RL_LINE_INDEX] IN (N'(5)', N'(10)', N'(15)', N'(17)', N'(20)')
  AND [RL_REPORT_TYPE] = N'CF';
