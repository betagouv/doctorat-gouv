-- Homogénéise les e-mails (ADUM/AMETHIS fournissent des casses variables :
-- MonMail@Domaine.fr vs MONMAIL@domaine.fr). Sans cela, un directeur inscrit avec
-- une casse différente ne peut ni se connecter ni voir ses sujets sur /mes-sujets-directeur-these.
-- La recherche /mes-sujets est déjà insensible à la casse (LOWER+TRIM), mais on
-- stocke désormais la forme canonique trim + minuscules pour fiabiliser
-- l'authentification et l'affichage.

UPDATE utilisateur
SET email = LOWER(TRIM(email))
WHERE email IS NOT NULL
  AND email <> LOWER(TRIM(email));

UPDATE proposition_these
SET direction_these_email = LOWER(TRIM(direction_these_email))
WHERE direction_these_email IS NOT NULL
  AND direction_these_email <> LOWER(TRIM(direction_these_email));

UPDATE proposition_these
SET codirection_these_email = LOWER(TRIM(codirection_these_email))
WHERE codirection_these_email IS NOT NULL
  AND codirection_these_email <> LOWER(TRIM(codirection_these_email));

UPDATE proposition_these
SET deposant_email = LOWER(TRIM(deposant_email))
WHERE deposant_email IS NOT NULL
  AND deposant_email <> LOWER(TRIM(deposant_email));
