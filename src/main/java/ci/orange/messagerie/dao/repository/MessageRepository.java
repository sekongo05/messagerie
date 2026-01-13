

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

}
