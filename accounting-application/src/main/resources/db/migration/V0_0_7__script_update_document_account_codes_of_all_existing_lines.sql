GO

Create FUNCTION increment_document_account_code_by_one
(@CodeToIncrement varchar(255)   
)  
RETURNS varchar(255)
AS  
BEGIN  
       DECLARE @IncrementedCode varchar(255)  
	   DECLARE @IncrementedValue int = CONVERT(varchar,CONVERT(int, @CodeToIncrement))+ 1
set @IncrementedCode = SUBSTRING('0000',1, 4 - LEN(convert(varchar,@IncrementedValue))) + Convert(varchar,@IncrementedValue)
RETURN @IncrementedCode  END;

GO 


CREATE PROCEDURE update_document_account_codes_of_all_existing_lines
      AS
BEGIN

DECLARE @ID bigint, @CURRENT_FISCAL_YEAR_ID bigint, @CURRENT_CODE_DOCUMENT varchar(255), @MONTH_COUNTER int;

DECLARE fiscal_year_id_cursor CURSOR FOR  
SELECT distinct DA_FISCAL_YEAR_ID FROM T_DOCUMENT_ACCOUNT
WHERE DA_IS_DELETED=0

OPEN fiscal_year_id_cursor;  
  FETCH NEXT FROM fiscal_year_id_cursor INTO @CURRENT_FISCAL_YEAR_ID;  
  WHILE @@FETCH_STATUS = 0  
  BEGIN  

		SET @MONTH_COUNTER = 1;
		WHILE @MONTH_COUNTER <= 12
		BEGIN
		
		   DECLARE document_account_id_cursor CURSOR FOR  
		   SELECT DA_ID FROM T_DOCUMENT_ACCOUNT
		   WHERE DA_FISCAL_YEAR_ID=@CURRENT_FISCAL_YEAR_ID AND MONTH(DA_DOCUMENT_DATE)=@MONTH_COUNTER AND DA_IS_DELETED=0
		   ORDER BY DA_DOCUMENT_DATE,DA_ID
		   
		   OPEN document_account_id_cursor;  
		   
		     set @CURRENT_CODE_DOCUMENT='0001'	
		     
		     FETCH NEXT FROM document_account_id_cursor INTO @ID;  
		     WHILE @@FETCH_STATUS = 0  
		     BEGIN  
		         IF @MONTH_COUNTER < 10
		         BEGIN
		   		UPDATE T_DOCUMENT_ACCOUNT SET DA_CODE_DOCUMENT =CONCAT('0',@MONTH_COUNTER,'/',@CURRENT_CODE_DOCUMENT) WHERE DA_FISCAL_YEAR_ID=@CURRENT_FISCAL_YEAR_ID AND MONTH(DA_DOCUMENT_DATE)=@MONTH_COUNTER AND DA_IS_DELETED=0 AND DA_ID = @ID
		         END
		     	  ELSE
		     	    BEGIN
		   		UPDATE T_DOCUMENT_ACCOUNT SET DA_CODE_DOCUMENT =CONCAT(@MONTH_COUNTER,'/',@CURRENT_CODE_DOCUMENT) WHERE DA_FISCAL_YEAR_ID=@CURRENT_FISCAL_YEAR_ID AND MONTH(DA_DOCUMENT_DATE)=@MONTH_COUNTER AND DA_IS_DELETED=0 AND DA_ID = @ID
		         END
		     
		        Select @CURRENT_CODE_DOCUMENT = dbo.[increment_document_account_code_by_one](@CURRENT_CODE_DOCUMENT)
		        
		     FETCH NEXT FROM document_account_id_cursor INTO @ID;  
		     END  

		   CLOSE document_account_id_cursor;  
		   DEALLOCATE document_account_id_cursor;  
		  
		   SET @MONTH_COUNTER = @MONTH_COUNTER + 1;
		END;

  FETCH NEXT FROM fiscal_year_id_cursor INTO @CURRENT_FISCAL_YEAR_ID;  		
  END
    
CLOSE fiscal_year_id_cursor;  
DEALLOCATE fiscal_year_id_cursor;  

END;

GO

EXEC update_document_account_codes_of_all_existing_lines;

DROP FUNCTION increment_document_account_code_by_one

GO

DROP PROCEDURE update_document_account_codes_of_all_existing_lines

GO
