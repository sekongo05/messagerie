import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

// Instance Axios configurée
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'lang': 'fr'
  },
  timeout: 10000
});

// Types
export interface MessageDto {
  id?: number;
  content?: string;
  imgUrl?: string;
  conversationId: number;
  typeMessage?: number;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: number;
  typeMessageLibelle?: string;
  typeMessageCode?: string;
}

export interface ApiResponse<T> {
  hasError: boolean;
  status: {
    code: string;
    message: string;
  };
  items: T[];
}

// Service pour les messages
export const messageApi = {
  /**
   * Récupérer les messages d'une conversation
   */
  async getByConversation(conversationId: number, userId: number = 1): Promise<MessageDto[]> {
    try {
      const response = await api.post<ApiResponse<MessageDto>>('/message/getByCriteria', {
        user: userId,
        datas: [
          {
            conversationId
          }
        ]
      });

      if (response.data.hasError) {
        throw new Error(response.data.status.message);
      }

      return response.data.items || [];
    } catch (error: any) {
      console.error('Erreur lors de la récupération des messages:', error);
      throw error;
    }
  },

  /**
   * Envoyer un message avec fichier (multipart)
   */
  async sendWithFile(
    formData: FormData,
    conversationId: number,
    userId: number = 1
  ): Promise<MessageDto> {
    try {
      // Ajouter les paramètres requis au FormData
      formData.append('conversationId', conversationId.toString());
      if (userId) {
        formData.append('user', userId.toString());
      }

      const response = await api.post<ApiResponse<MessageDto>>(
        '/message/create-with-file',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
            'lang': 'fr'
          }
        }
      );

      if (response.data.hasError) {
        throw new Error(response.data.status.message);
      }

      return response.data.items?.[0];
    } catch (error: any) {
      console.error('Erreur lors de l\'envoi du message avec fichier:', error);
      throw error;
    }
  },

  /**
   * Envoyer un message texte uniquement (JSON)
   */
  async send(
    content: string,
    conversationId: number,
    typeMessage: number = 1,
    userId: number = 1
  ): Promise<MessageDto> {
    try {
      const response = await api.post<ApiResponse<MessageDto>>('/message/create', {
        user: userId,
        datas: [
          {
            content,
            conversationId,
            typeMessage
          }
        ]
      });

      if (response.data.hasError) {
        throw new Error(response.data.status.message);
      }

      return response.data.items?.[0];
    } catch (error: any) {
      console.error('Erreur lors de l\'envoi du message:', error);
      throw error;
    }
  }
};
