

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
import ci.orange.messagerie.dao.repository.base._ConversationRepository;

/**
 * Repository : Conversation.
 *
 * @author Geo
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer>, _ConversationRepository {
     // permet de resortir le type d'une conversation'
    @Query("""
    SELECT e.typeConversation
    FROM Conversation e
    WHERE e.id = :conversationId
      AND e.isDeleted = false
""")
    TypeConversation findConversationType(@Param("conversationId") Integer conversationId);

    /**
     * Récupère le dernier message d'une conversation
     * @param conversationId L'ID de la conversation
     * @return Le dernier message de la conversation (le plus récent par date de création)
     */
    @Query("""
        SELECT m
        FROM Message m
        WHERE m.conversation.id = :conversationId
          AND m.isDeleted = false
          AND m.createdAt = (
              SELECT MAX(m2.createdAt)
              FROM Message m2
              WHERE m2.conversation.id = :conversationId
                AND m2.isDeleted = false
          )
    """)
    Message findLastMessageByConversationId(@Param("conversationId") Integer conversationId);

    /**
     * Récupère le nom de l'expéditeur et du destinataire pour une conversation privée
     * @param conversationId L'ID de la conversation privée
     * @param messageCreatedBy L'ID de l'utilisateur qui a créé le message (expéditeur)
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
        JOIN User sender ON sender.id = :messageCreatedBy
        JOIN User recipient ON (
            (pc1.user.id = :messageCreatedBy AND recipient.id = pc2.user.id)
            OR
            (pc2.user.id = :messageCreatedBy AND recipient.id = pc1.user.id)
        )
        WHERE pc1.conversation.id = :conversationId
          AND pc1.conversation.isDeleted = false
          AND pc1.isDeleted = false
          AND pc2.isDeleted = false
          AND pc1.user.id <> pc2.user.id
          AND (pc1.user.id = :messageCreatedBy OR pc2.user.id = :messageCreatedBy)
    """)
    List<Object[]> findSenderAndRecipientNamesForPrivateConversation(
        @Param("conversationId") Integer conversationId,
        @Param("messageCreatedBy") Integer messageCreatedBy
    );

    /**
     * Récupère l'ID et le nom de l'interlocuteur pour une conversation privée
     * @param conversationId L'ID de la conversation privée
     * @param currentUserId L'ID de l'utilisateur actuel
     * @return Un tableau Object[] contenant :
     *         [0] = ID de l'interlocuteur (Integer)
     *         [1] = nom de l'interlocuteur (String)
     *         [2] = prénoms de l'interlocuteur (String)
     *         Retourne une liste vide si la conversation n'est pas privée ou si l'interlocuteur n'est pas trouvé
     */
    @Query("""
        SELECT 
            interlocuteur.id,
            interlocuteur.nom,
            interlocuteur.prenoms
        FROM ParticipantConversation pc1
        JOIN ParticipantConversation pc2 ON pc2.conversation.id = pc1.conversation.id
        JOIN User interlocuteur ON (
            (pc1.user.id = :currentUserId AND interlocuteur.id = pc2.user.id)
            OR
            (pc2.user.id = :currentUserId AND interlocuteur.id = pc1.user.id)
        )
        WHERE pc1.conversation.id = :conversationId
          AND pc1.conversation.isDeleted = false
          AND pc1.isDeleted = false
          AND pc2.isDeleted = false
          AND pc1.user.id <> pc2.user.id
          AND (pc1.user.id = :currentUserId OR pc2.user.id = :currentUserId)
    """)
    List<Object[]> findInterlocutorForPrivateConversation(
        @Param("conversationId") Integer conversationId,
        @Param("currentUserId") Integer currentUserId
    );

}
