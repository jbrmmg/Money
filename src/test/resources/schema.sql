/* Schema for testing. */
/* account */
CREATE TABLE `account` (
  `id` char(4) NOT NULL,
  `name` varchar(45) NOT NULL,
  `image_prefix` varchar(45) NOT NULL,
  `colour` char(6) NOT NULL,
  PRIMARY KEY (`id`)
);

/* category */
CREATE TABLE `category` (
  `id` char(3) NOT NULL,
  `name` varchar(45) NOT NULL,
  `sort` int(11) NOT NULL,
  `restricted` char(1) NOT NULL,
  `expense` char(1) NOT NULL,
  `colour` char(6) NOT NULL,
  `groupid` varchar(45) NOT NULL,
  `system_use` char(1) NOT NULL,
  PRIMARY KEY (`id`)
);

/* reconciliation data */
CREATE TABLE `reconciliation_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category` char(3) NOT NULL,
  `date` datetime NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `description` varchar(40) NOT NULL,
  `colour` char(6) NOT NULL,
  PRIMARY KEY (`id`)
);

/* statement */
CREATE TABLE `statement` (
  `account` char(4) NOT NULL,
  `month` int(11) NOT NULL,
  `year` int(11) NOT NULL,
  `open_balance` decimal(10,2) DEFAULT NULL,
  `locked` char(1) DEFAULT NULL,
  PRIMARY KEY (`account`,`month`,`year`)
);

/* transaction */
CREATE TABLE `transaction` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `account` char(4) NOT NULL,
  `category` char(3) NOT NULL,
  `date` datetime NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `statement` char(7) DEFAULT NULL,
  `oppositeid` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `regular` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`account` char(4) NOT NULL,
`amount` decimal(10,2) NOT NULL,
`category` char(3) NOT NULL,
`frequency` char(2) NOT NULL,
`weekend_adj` char(2) NOT NULL,
`start` datetime NOT NULL,
`last_created` datetime DEFAULT NULL,
PRIMARY KEY (`id`)
);



/*
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER
*/
/* all_transaction view */
CREATE
VIEW `all_transaction` AS
select `t`.`id` AS `id`,
       `t`.`date` AS `date`,
       `t`.`amount` AS `amount`,
       `t`.`oppositeid` AS `opposite_id`,
       ifnull(`t`.`statement`,'') AS `statement_id`,
       ifnull(`s`.`locked`,'N') AS `locked`,
       `t`.`account` AS `account_id`,
       `t`.`category` AS `category_id`,
       `c`.`name` AS `category_name`,
       `c`.`colour` AS `colour`,
       `c`.`groupid` AS `category_group`,
      ifnull(`o`.`statement`,'') AS `opp_statement_id`
from (((`transaction` `t`
  left join `statement` `s` on(((`t`.`account` = `s`.`account`) and (`t`.`statement` = ((`s`.`year` * 100) + `s`.`month`)))))
  left join `category` `c` on((`t`.`category` = `c`.`id`)))
  left join `transaction` `o` on((`t`.`id` = `o`.`oppositeid`)));
