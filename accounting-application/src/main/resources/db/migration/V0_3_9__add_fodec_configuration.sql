SET IDENTITY_INSERT [dbo].[T_ACCOUNTING_CONFIGURATION] ON

ALTER TABLE [dbo].[T_ACCOUNTING_CONFIGURATION]
ADD 	[AC_FODEC_PURCHASES_ACCOUNT_ID] [bigint] NULL,
	[AC_FODEC_SALES_ACCOUNT_ID] [bigint] NULL

SET IDENTITY_INSERT [dbo].[T_ACCOUNTING_CONFIGURATION] OFF
