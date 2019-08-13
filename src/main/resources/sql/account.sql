CREATE TABLE `account` (
  `id` char(4) NOT NULL,
  `name` varchar(45) NOT NULL,
  `image_prefix` varchar(45) NOT NULL,
  `colour` char(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
