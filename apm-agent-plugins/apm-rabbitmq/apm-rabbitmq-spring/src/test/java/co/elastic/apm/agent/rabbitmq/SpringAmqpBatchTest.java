package co.elastic.apm.agent.rabbitmq;


import co.elastic.apm.agent.impl.transaction.Transaction;
import co.elastic.apm.agent.rabbitmq.config.BatchConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.BatchingRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {BatchConfiguration.class}, initializers = {RabbitMqTestBase.Initializer.class})
public class SpringAmqpBatchTest extends RabbitMqTestBase {

    @Autowired
    private BatchingRabbitTemplate batchingRabbitTemplate;

    @Test
    public void verifyThatTransactionWithSpanCreated() {
        batchingRabbitTemplate.convertAndSend(TestConstants.QUEUE_NAME, "hello");
        batchingRabbitTemplate.convertAndSend(TestConstants.QUEUE_NAME, "hello");
        batchingRabbitTemplate.convertAndSend(TestConstants.QUEUE_NAME, "hello");
        batchingRabbitTemplate.convertAndSend(TestConstants.QUEUE_NAME, "hello");

        getReporter().awaitTransactionCount(4);

        List<Transaction> transactionList = getReporter().getTransactions();

        assertThat(transactionList.size()).isEqualTo(4);
        for (Transaction transaction : transactionList) {
            assertThat(transaction.getNameAsString()).isEqualTo("Spring AMQP RECEIVE from <default>");
            assertThat(transaction.getSpanCount().getTotal().get()).isEqualTo(1);
            assertThat(getReporter().getFirstSpan().getNameAsString()).isEqualTo("testSpan");
        }
    }
}