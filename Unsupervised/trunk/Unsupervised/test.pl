#use lib './lib/Encoding-FixLatin-1.02/lib';
#use lib 'C:/Users/updates/workspace-CharaParser/Unsupervised/lib/';

use lib 'lib';
use Encoding::FixLatin qw(fix_latin);

my $line=" corollas (3-)5-merous, ± actinomorphic or zygomorphic ";
$line = fix_latin($line);
print $line;
	
