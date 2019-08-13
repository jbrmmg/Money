CREATE
    ALGORITHM = UNDEFINED
    DEFINER = `root`@`192.168.1.64`
    SQL SECURITY DEFINER
VIEW `all_transaction` AS
    SELECT
        `t`.`id` AS `id`,
        `t`.`date` AS `date`,
        `t`.`amount` AS `amount`,
        `t`.`oppositeid` AS `opposite_id`,
        IFNULL(`t`.`statement`, '') AS `statement_id`,
        IFNULL(`s`.`locked`, 'N') AS `locked`,
        `t`.`account` AS `account_id`,
        `t`.`category` AS `category_id`,
        `c`.`name` AS `category_name`,
        `c`.`colour` AS `colour`,
        `c`.`groupid` AS `category_group`,
        IFNULL(`o`.`statement`, '') AS `opp_statement_id`
    FROM
        (((`transaction` `t`
        LEFT JOIN `statement` `s` ON (((`t`.`account` = `s`.`account`)
            AND (`t`.`statement` = ((`s`.`year` * 100) + `s`.`month`)))))
        LEFT JOIN `category` `c` ON ((`t`.`category` = `c`.`id`)))
        LEFT JOIN `transaction` `o` ON ((`t`.`id` = `o`.`oppositeid`)))