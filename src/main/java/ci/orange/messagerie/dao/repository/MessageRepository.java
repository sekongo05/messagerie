

package ci.orange.messagerie.dao.repository;

import java.util.Date;
import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Locale;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ci.orange.messagerie.utils.*;
import ci.orange.messagerie.utils.dto.*;
import ci.orange.messagerie.utils.contract.*;
import ci.orange.messagerie.utils.contract.Request;
import ci.orange.messagerie.utils.contract.Response;
import ci.orange.messagerie.dao.entity.*;
import ci.orange.messagerie.dao.repository.base._MessageRepository;

/**
 * Repository : Message.
 *
 * @author Geo
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Integer>, _MessageRepository {

    /**
     * Récupère l'heure d'envoi d'un message
     * @param messageId L'ID du message
     * @return La date/heure de création du message (heure d'envoi), ou null si le message n'existe pas ou est supprimé
     */
    @Query("""
        SELECT m.createdAt
        FROM Message m
        WHERE m.id = :messageId
          AND m.isDeleted = false
    """)
    Date findMessageSendTime(@Param("messageId") Integer messageId);

    /**
     * Récupère les noms de l'expéditeur et du destinataire pour un message dans une conversation privée
     * @param conversationId L'ID de la conversation
     * @param senderId L'ID de l'utilisateur qui a envoyé le message (expéditeur)
     * @return Un tableau Object[] contenant :
     *         [0] = nom de l'expéditeur (String)
     *         [1] = prénoms de l'expéditeur (String)
     *         [2] = nom du destinataire (String)
     *         [3] = prénoms du destinataire (String)
     *         Retourne une liste vide si la conversation n'est pas privée ou si les participants ne sont pas trouvés
     */
    @Query("""
        SELECT 
            sender.nom,
            sender.prenoms,
            recipient.nom,
            recipient.prenoms
        FROM ParticipantConversation pc1
        JOIN ParticipantConversation pc2 ON pc2.conversation.id = pc1.conversation.id
        JOIN User sender ON sender.id = :senderId AND sender.isDeleted = false
        JOIN User recipient ON (
            (pc1.user.id = :senderId AND recipient.id = pc2.user.id)
            OR
            (pc2.user.id = :senderId AND recipient.id = pc1.user.id)
        ) AND recipient.isDeleted = false
        WHERE pc1.conversation.id = :conversationId
          AND pc1.conversation.isDeleted = false
          AND pc1.isDeleted = false
          AND pc2.isDeleted = false
          AND pc1.user.id <> pc2.user.id
          AND (pc1.user.id = :senderId OR pc2.user.id = :senderId)
    """)
    List<Object[]> findSenderAndRecipientNamesForMessage(
        @Param("conversationId") Integer conversationId,
        @Param("senderId") Integer senderId
    );

    public default List<Message> getByCriteriaCustomise(Request<MessageDto> request, EntityManager em, Locale locale) throws DataAccessException, Exception {
        String req = "select e from Message e where e IS NOT NULL";
        HashMap<String, Object> param = new HashMap<String, Object>();
        String whereExpression = getWhereExpression(request, param, locale);
        
        // Extraire la clause ORDER BY de whereExpression si elle existe
        String orderByClause = "";
        String whereClause = whereExpression;
        int orderByIndex = whereExpression.toLowerCase().indexOf(" order by ");
        if (orderByIndex >= 0) {
            orderByClause = whereExpression.substring(orderByIndex);
            whereClause = whereExpression.substring(0, orderByIndex);
        }
        
        // Construire la requête avec la condition d'exclusion de l'historique
        req += whereClause;
        
        // Exclure les messages qui sont dans l'historique de suppression pour l'utilisateur courant
        Integer userId = request.getUser();
        if (userId != null && userId > 0) {
            req += " AND NOT EXISTS (SELECT 1 FROM HistoriqueSuppressionMessage h WHERE h.message.id = e.id AND h.user.id = :historiqueUserId AND (h.isDeleted = false OR h.isDeleted IS NULL))";
            param.put("historiqueUserId", userId);
        }
        
        // Ré-ajouter la clause ORDER BY à la fin
        req += orderByClause;
        
        TypedQuery<Message> query = em.createQuery(req, Message.class);
        for (Map.Entry<String, Object> entry : param.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        if (request.getIndex() != null && request.getSize() != null) {
            query.setFirstResult(request.getIndex() * request.getSize());
            query.setMaxResults(request.getSize());
        }
        return query.getResultList();
    }


}
