ALTER TABLE [T_JOURNAL]
    DROP CONSTRAINT [UK_48k0b0ocnhjwhh7ggcw6gacby]
GO
ALTER TABLE [T_JOURNAL]
    DROP CONSTRAINT [UK_b5yybeg4eiufn90qpktu4x7yu]
GO
ALTER TABLE [T_JOURNAL]
    ADD CONSTRAINT
        [UniqueJournalCode] UNIQUE NONCLUSTERED ([JN_DELETED_TOKEN] ASC, [JN_CODE] ASC)
GO

ALTER TABLE [T_JOURNAL]
    ADD CONSTRAINT
        [UniqueJournalLabel] UNIQUE NONCLUSTERED ([JN_DELETED_TOKEN] ASC, [JN_LABEL] ASC)
GO