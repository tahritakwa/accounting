SET IDENTITY_INSERT [dbo].[T_ACCOUNTING_CONFIGURATION] ON

ALTER TABLE [dbo].[T_ACCOUNTING_CONFIGURATION] DROP CONSTRAINT FKn733smef7t1b0bjdakm888llm;
ALTER TABLE [dbo].[T_ACCOUNTING_CONFIGURATION] DROP CONSTRAINT FKhhclhc3q32feengo84nnqwmdm;

ALTER TABLE [dbo].[T_ACCOUNTING_CONFIGURATION] DROP COLUMN [AC_BALANCE_SHEET_CLOSING_ACCOUNT_ID],[AC_BALANCE_SHEET_OPENNING_ACCOUNT_ID]; 
 	
SET IDENTITY_INSERT [dbo].[T_ACCOUNTING_CONFIGURATION] OFF

 