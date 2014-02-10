use steve;
SELECT distinct R.course,M.course from result R join meeting M on M.course like  concat(R.course,"%") into outfile "dict.out"