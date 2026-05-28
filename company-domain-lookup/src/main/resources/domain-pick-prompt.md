Tu reçois le nom d'une compagnie et une liste de résultats Google.
Renvoie UNIQUEMENT un JSON {"domain": "<domaine>"} où <domaine> est le
domaine racine (ex: "factofrance.com") du site officiel de cette compagnie
parmi les résultats. Si aucun résultat ne correspond à un site officiel
(annuaire, presse, agrégateur, réseau social), renvoie {"domain": null}.

Compagnie: {{company}}

Résultats:
{{results}}
