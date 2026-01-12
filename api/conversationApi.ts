import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

// Instance Axios configurée
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'lang': 'fr'
  },
  timeout: 10000
});

// Types
export interface ConversationDto {
  id?: number;
  titre?: string;
  typeConversationId?: number;
  typeConversationLibelle?: string;
  typeConversationCode?: string;
  createdAt?: string;
  updatedAt?: string;
  interlocuteurId?: number;
  messageContent?: string;
  messageImgUrl?: string;
}

export interface ApiResponse<T> {
  hasError: boolean;
  status: {
    code: string;
    message: string;
  };
  items: T[];
}

// Service pour les conversations
export const conversationApi = {
  /**
   * Récupérer toutes les conversations
   */
  async getAll(userId: number = 1): Promise<ConversationDto[]> {
    try {
      const response = await api.post<ApiResponse<ConversationDto>>('/conversation/getByCriteria', {
        user: userId,
        datas: [{}]
      });

      if (response.data.hasError) {
        throw new Error(response.data.status.message);
      }

      return response.data.items || [];
    } catch (error: any) {
      console.error('Erreur lors de la récupération des conversations:', error);
      throw error;
    }
  },

  /**
   * Rechercher des conversations par critères
   */
  async search(criteria: Partial<ConversationDto>, userId: number = 1): Promise<ConversationDto[]> {
    try {
      const response = await api.post<ApiResponse<ConversationDto>>('/conversation/getByCriteria', {
        user: userId,
        datas: [criteria]
      });

      if (response.data.hasError) {
        throw new Error(response.data.status.message);
      }

      return response.data.items || [];
    } catch (error: any) {
      console.error('Erreur lors de la recherche de conversations:', error);
      throw error;
    }
  },

  /**
   * Créer une nouvelle conversation
   */
  async create(titre: string, typeConversationId: number, userId: number = 1): Promise<ConversationDto> {
    try {
      const response = await api.post<ApiResponse<ConversationDto>>('/conversation/create', {
        user: userId,
        datas: [
          {
            titre,
            typeConversationId
          }
        ]
      });

      if (response.data.hasError) {
        throw new Error(response.data.status.message);
      }

      return response.data.items?.[0];
    } catch (error: any) {
      console.error('Erreur lors de la création de la conversation:', error);
      throw error;
    }
  }
};
