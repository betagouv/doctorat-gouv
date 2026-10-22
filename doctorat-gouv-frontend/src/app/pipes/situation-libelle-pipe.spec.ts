import { SituationLibellePipe } from './situation-libelle-pipe';

describe('SituationLibellePipe', () => {
  const pipe = new SituationLibellePipe();

  it('should be created', () => {
    expect(pipe).toBeTruthy();
  });

  it('traduit un code du nouveau référentiel en libellé', () => {
    expect(pipe.transform('MASTER_FRANCE')).toBe('Étudiante ou étudiant en master en France');
    expect(pipe.transform('CHERCHEUR_ENTREPRISE')).toBe('Chercheuse ou chercheur en entreprise');
    expect(pipe.transform('AUTRE')).toBe('Autre');
  });

  it('traduit un ancien code (legacy) en libellé', () => {
    expect(pipe.transform('ETUDIANT_MASTER')).toBe('Étudiant en master');
    expect(pipe.transform('TITULAIRE_MASTER')).toBe("Titulaire d'un master");
    expect(pipe.transform('DOCTORANT')).toBe('Doctorant');
    expect(pipe.transform('PROFESSIONNEL')).toBe('Professionnel');
  });

  it('affiche la valeur brute si elle est inconnue', () => {
    expect(pipe.transform('CODE_INCONNU')).toBe('CODE_INCONNU');
    expect(pipe.transform('Chercheur')).toBe('Chercheur');
  });

  it('affiche un tiret si la valeur est vide', () => {
    expect(pipe.transform(null)).toBe('—');
    expect(pipe.transform('')).toBe('—');
    expect(pipe.transform(undefined)).toBe('—');
  });
});
