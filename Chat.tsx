import { useState, useEffect } from 'react';
import { ConversationList } from '../Metier/Conversation/ConversationList';
import { MessagesList } from '../Metier/Messages/MessagesList';
import MessageInput from '../Metier/Messages/MessageInput';
import { useTheme } from '../mode';
import { FiLoader } from "react-icons/fi";
import { CgProfile } from "react-icons/cg";
import { CgExport } from "react-icons/cg";
import { conversationApi, ConversationDto } from '../api/conversationApi';
import { messageApi, MessageDto } from '../api/messageApi';

// Types
type Conversation = {
  id: number;
  name: string;
  lastMessage?: string;
  lastMessageTime?: string;
  unreadCount?: number;
  avatar?: string;
};

type Message = {
  id: number;
  content?: string;
  image?: string;
  senderId: number;
  senderName: string;
  timestamp: string;
  typeMessage: '1' | '2' | '3';
  conversationId: number;
};

type ChatProps = {
  onNavigateToProfile?: () => void;
};

const Chat = ({ onNavigateToProfile }: ChatProps = {}) => {
  const { theme } = useTheme();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<number | null>(null);
  const [currentUserId] = useState<number>(1); // À remplacer par l'ID de l'utilisateur connecté
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Charger les conversations au montage
  useEffect(() => {
    loadConversations();
  }, []);

  // Charger les messages quand une conversation est sélectionnée
  useEffect(() => {
    if (activeConversationId) {
      loadMessages(activeConversationId);
    }
  }, [activeConversationId]);

  // Charger les conversations depuis l'API
  const loadConversations = async () => {
    setLoading(true);
    setError(null);
    try {
      const conversationsData = await conversationApi.getAll(currentUserId);
      
      // Transformer les données de l'API en format Conversation
      const transformedConversations: Conversation[] = conversationsData.map((conv: ConversationDto) => ({
        id: conv.id || 0,
        name: conv.titre || 'Sans titre',
        lastMessage: conv.messageContent || '',
        lastMessageTime: conv.createdAt ? formatTime(conv.createdAt) : '',
        unreadCount: 0, // À implémenter selon vos besoins
        avatar: undefined
      }));

      setConversations(transformedConversations);
    } catch (error: any) {
      console.error('Erreur lors du chargement des conversations:', error);
      setError(error.message || 'Erreur lors du chargement des conversations');
    } finally {
      setLoading(false);
    }
  };

  // Charger les messages depuis l'API
  const loadMessages = async (conversationId: number) => {
    setLoading(true);
    setError(null);
    try {
      const messagesData = await messageApi.getByConversation(conversationId, currentUserId);
      
      // Transformer les données de l'API en format Message
      const transformedMessages: Message[] = messagesData.map((msg: MessageDto) => ({
        id: msg.id || 0,
        content: msg.content || undefined,
        image: msg.imgUrl || undefined,
        senderId: msg.createdBy || 0,
        senderName: msg.createdBy === currentUserId ? 'Moi' : 'Utilisateur',
        timestamp: msg.createdAt || new Date().toISOString(),
        typeMessage: (msg.typeMessage?.toString() || '1') as '1' | '2' | '3',
        conversationId: msg.conversationId
      }));

      setMessages(transformedMessages);
    } catch (error: any) {
      console.error('Erreur lors du chargement des messages:', error);
      setError(error.message || 'Erreur lors du chargement des messages');
    } finally {
      setLoading(false);
    }
  };

  // Formater l'heure pour l'affichage
  const formatTime = (dateString: string): string => {
    try {
      // Format attendu: "dd/MM/yyyy" ou ISO string
      const date = new Date(dateString);
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      return `${hours}:${minutes}`;
    } catch {
      return '';
    }
  };

  // Gérer la sélection d'une conversation
  const handleConversationSelect = (conversationId: number) => {
    setActiveConversationId(conversationId);
  };

  // Gérer la fermeture d'une conversation
  const handleCloseConversation = () => {
    setActiveConversationId(null);
    setMessages([]);
  };

  // Gérer l'envoi d'un message
  const handleSendMessage = async (formData: FormData) => {
    if (!activeConversationId) return;

    try {
      setError(null);
      
      // Vérifier si un fichier est présent dans le FormData
      const hasFile = formData.has('file');
      const content = formData.get('content') as string;

      if (hasFile) {
        // Envoyer avec fichier (multipart)
        await messageApi.sendWithFile(formData, activeConversationId, currentUserId);
      } else if (content && content.trim()) {
        // Envoyer message texte uniquement (JSON)
        await messageApi.send(content.trim(), activeConversationId, 1, currentUserId);
      } else {
        throw new Error('Le message doit contenir du texte ou un fichier');
      }
      
      // Recharger les messages après envoi
      await loadMessages(activeConversationId);
      
      // Recharger les conversations pour mettre à jour le dernier message
      await loadConversations();
    } catch (error: any) {
      console.error('Erreur lors de l\'envoi du message:', error);
      setError(error.message || 'Erreur lors de l\'envoi du message');
      throw error;
    }
  };

  const bgColor = theme === 'dark' ? 'bg-gray-900' : 'bg-gray-100';
  const borderColor = theme === 'dark' ? 'border-gray-700' : 'border-gray-300';

  return (
    <div className={`h-screen flex  ${bgColor}`}>
      {/* Sidebar - Liste des conversations */}
      <div className={`w-120 border-r ${borderColor} flex flex-col ${theme === 'dark' ? 'bg-gray-800' : 'bg-white'}`}>
        <div className={`p-4 border-b ${borderColor} flex items-center justify-between`}>
          <h2 className={`text-[40px] font-bold ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>
            Discussions
          </h2>
          <button
              
              className={`p-2 rounded-lg transition-colors ${
                theme === 'dark'
                  ? 'text-gray-400 hover:bg-gray-700 hover:text-white'
                  : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
              }`}
              
            >
              <CgExport className='w-8 h-10' />

            </button>
          {onNavigateToProfile && (
            <button
              onClick={onNavigateToProfile}
              className={`p-2 rounded-lg transition-colors ${
                theme === 'dark'
                  ? 'text-gray-400 hover:bg-gray-700 hover:text-white'
                  : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
              }`}
              title="Voir mon profil"
            >
              <CgProfile className='w-8 h-10' />
            </button>
          )}
        </div>
        {error && (
          <div className={`p-2 m-2 rounded ${theme === 'dark' ? 'bg-red-900 text-red-200' : 'bg-red-100 text-red-800'}`}>
            {error}
          </div>
        )}
        {loading && conversations.length === 0 ? (
          <div className="flex-1 border-2 flex items-center justify-center">
            <p className={theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}><FiLoader /></p>
          </div>
        ) : (
          <ConversationList
            conversations={conversations}
            activeConversationId={activeConversationId || undefined}
            onConversationSelect={handleConversationSelect}
            theme={theme}
          />
        )}
      </div>

      {/* Zone principale - Messages */}
      <div className="flex-1 flex  flex-col">
        {activeConversationId ? (
          <>
            {/* En-tête de la conversation */}
            <div className={`p-4 border-b ${borderColor} ${theme === 'dark' ? 'bg-gray-800' : 'bg-white'} flex  items-center justify-between`}>
              {(() => {
                const conversation = conversations.find(c => c.id === activeConversationId);
                return (
                  <>
                    <h3 className={`text-lg font-semibold ${theme === 'dark' ? 'text-white' : 'text-gray-900'}`}>
                      {conversation?.name || 'Conversation'}
                    </h3>
                    <div className="flex  items-center gap-2">
                      
                      <button
                        onClick={handleCloseConversation}
                        className={`p-2 rounded-lg transition-colors ${
                          theme === 'dark'
                            ? 'text-gray-400 hover:bg-gray-700 hover:text-white'
                            : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                        }`}
                        title="Fermer la conversation"
                        aria-label="Fermer la conversation"
                      >
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          className="h-5 w-5"
                          viewBox="0 0 20 20"
                          fill="currentColor"
                        >
                          <path
                            fillRule="evenodd"
                            d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                            clipRule="evenodd"
                          />
                        </svg>
                      </button>
                    </div>
                  </>
                );
              })()}
            </div>

            {/* Liste des messages */}
            {loading ? (
              <div className="flex-1 flex items-center justify-center">
                <p className={theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}><FiLoader /></p>
              </div>
            ) : (
              <MessagesList
                messages={messages}
                currentUserId={currentUserId}
                theme={theme}
              />
            )}

            {/* Input pour envoyer un message */}
            <MessageInput
              conversationId={activeConversationId}
              userId={currentUserId}
              onSend={handleSendMessage}
              theme={theme}
            />
          </>
        ) : (
          <div className="flex-1 flex items-center justify-center">
            <p className={theme === 'dark' ? 'text-gray-400' : 'text-gray-500'}>
              Sélectionnez une conversation pour commencer
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Chat;
