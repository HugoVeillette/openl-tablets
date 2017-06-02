create SEQUENCE OpenLGroups_ID_SEQ;
create or replace trigger OpenLGroups_ID_TRG
before insert on OpenLGroups
for each row
begin
  if :new.id is null then
    select OpenLGroups_ID_SEQ.nextval into :new.id from dual;
  end if;
end;
