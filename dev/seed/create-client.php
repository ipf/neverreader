#!/usr/bin/env php
<?php
/**
 * Create an OAuth client for the seeded Wallabag user.
 *
 * The bundled `fos:oauth-server:create-client` command is broken in this
 * version: Wallabag's Client entity takes a User in its constructor, so the
 * command dies with an ArgumentCountError before it can do anything. This does
 * the same job through Doctrine, which is also what Wallabag's own
 * POST /api/clients does, so the field encodings are the ones the server expects.
 *
 * Usage, inside the wallabag container:
 *   php /seed/create-client.php <username> <random_id> <secret>
 */

$root = getenv('WALLABAG_ROOT') ?: '/var/www/wallabag';
require $root . '/vendor/autoload.php';
require $root . '/app/AppKernel.php';

use Doctrine\ORM\EntityManagerInterface;
use Wallabag\ApiBundle\Entity\Client;
use Wallabag\UserBundle\Entity\User;

if ($argc < 4) {
    fwrite(STDERR, "usage: create-client.php <username> <random_id> <secret>\n");
    exit(1);
}
[$_, $username, $randomId, $secret] = $argv;

$kernel = new AppKernel('prod', false);
$kernel->boot();
$container = $kernel->getContainer();

/** @var EntityManagerInterface $em */
$em = $container->get('doctrine')->getManager();

$user = $em->getRepository(User::class)->findOneBy(['username' => $username]);
if ($user === null) {
    fwrite(STDERR, "no such user: $username\n");
    exit(1);
}

// Reuse an existing client for this user, so re-running seed.sh does not pile
// up duplicate rows with different public ids.
$repo = $em->getRepository(Client::class);
foreach ($repo->findBy(['user' => $user, 'randomId' => $randomId]) as $existing) {
    printf("client already exists\n");
    printf("  client_id     = %s\n", $existing->getClientId());
    printf("  client_secret = %s\n", $secret);
    printf("\nPut the client_id in the app as WALLABAG_CLIENT_ID.\n");
    exit(0);
}

$client = new Client($user);
$client->setName('NeverReader');
$client->setRandomId($randomId);
$client->setSecret($secret);
$client->setRedirectUris([]);
$client->setAllowedGrantTypes(['password', 'refresh_token']);

$em->persist($client);
$em->flush();

// The public id the token endpoint expects is "<id>_<randomId>", which is why a
// bare client name can never work: ClientManager::findClientByPublicId
// requires the underscore and returns null without it.
printf("client created\n");
printf("  client_id     = %s\n", $client->getClientId());
printf("  client_secret = %s\n", $secret);
printf("\nPut the client_id in the app as WALLABAG_CLIENT_ID.\n");
