UPDATE [dbo].[T_STANDARD_REPORT_LINE]
   SET 
      [SRL_LABEL] = N'Emprunts et dettes assimilés'
 WHERE [SRL_ANNEX_CODE] LIKE N'207' AND [SRL_REPORT_TYPE] LIKE N'BS';
 	
