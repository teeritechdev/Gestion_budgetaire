export interface Structure {
  code: string;
  nom: string;
  libelle?: string;
  telephone?: string | null;
  adresse?: string | null;
  email?: string | null;
  ville?: string | null;
  pays?: string | null;
  sigle?: string | null;
  secteurActivite?: string | null;
  formeJuridique?: string | null;
  numeroFiscal?: string | null;
  siteWeb?: string | null;
  createdAt?: string;
  updatedAt?: string;
}
