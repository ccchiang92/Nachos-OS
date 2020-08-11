"""NOTE: You can use this directly to test a specific log file, but
runtests.sh calls this script and runs a series of tests automatically.

Run as: python boattest.py [LOGFILE]

How to use:
    Run Nachos with a boat simulation and send the output to a log file. Then
run the command above. The first line of output will indicate whether the
simulation was a success or failure, or if the log file was unusable."""

from __future__ import print_function

import re
import sys

def run_test(logfile):
    bg = BoatGrader()
    ignored_lines = []
    try:
        for num, line in enumerate(open(logfile), start=1):
            if re.match(startpattern, line):
                bg.start(line, num)
            elif re.match(movepattern, line):
                bg.move(line, num)
            elif line.strip(): # empty lines are safe to ignore
                ignored_lines.append((line, num))
        bg.end(None, -1)
    except StopTest:
        pass
    if ignored_lines:
        print(IGNORED_MESSAGE)
        for line, num in ignored_lines:
            BoatGrader.printline(line, num)

### CONSTANTS ###

CHILD, ADULT, OAHU, MOLOKAI, ROW, RIDE = (
'child', 'adult', 'Oahu', 'Molokai', 'Row', 'Ride')
CHILDREN, ADULTS = 'children', 'adults'

raw_successor_table = {None:     '*m-row',
                      'am-row':  '*o-row',
                      'ao-row':  '*m-row',
                      'cm-row':  '*o-row or cm-ride',
                      'co-row':  '*m-row or co-ride',
                      'cm-ride': '*o-row',
                      'co-ride': '*m-row'}

child_or_children = '(child(ren)?)'
adult_or_adults = '(adult(s)?)'
people = '({}|{})'.format(child_or_children, adult_or_adults)
startpattern = (' *\**Testing Boats with '+
                '(only \d+ {}'.format(people)+
                '|\d+ {}, \d+ {})'.format(child_or_children, adult_or_adults))

islands = '(Molokai|Oahu)'
movepattern = (' *\**(Adult|Child) '+
               '(rowing to {}'.format(islands)+
               '|arrived on {} as a passenger)'.format(islands))

IGNORED_MESSAGE = '\nIMPORTANT: These lines (with line numbers) were ignored. Make sure they were not mistakenly ignored.'

### UTILITY FUNCTIONS ###

def move_str(move):
    return '{}{}To{}'.format(*move)

def otherisland(island):
    if island == OAHU:
        return MOLOKAI
    elif island == MOLOKAI:
        return OAHU
    else:
        assert False, 'Bad input to otherisland(): ' + island

def plural(age):
    if age == CHILD:
        return CHILDREN
    elif age == ADULT:
        return ADULTS
    else:
        assert False, 'Bad input to plural(): ' + age

### TESTING CODE ###

class StopTest(Exception): pass

class BoatGrader:
    """A BoatGrader keeps track of boat activity. Its non-static methods are
    called with a LOGLINE. They parse, carry out, and check the validity of a
    line. If invalid, report an error and stop the test."""
    def __init__(self):
        self.started = False

    def start(self, line, num):
        """Parse, carry out, and check the validity of a start."""
        if not self.started:
            self.started = True
        else:
            BoatGrader.fail(line, num, "Must have only one 'Testing Boats...' line.")

        # Parse
        populations = [int(n) for n in line.split() if n.isdigit()]
        if len(populations) == 2:
            children, adults = populations
        elif 'child' in line:
            children = populations[0]
            adults = 0
        elif 'adult' in line:
            adults = populations[0]
            children = 0

        # Carry out
        self.people = Population(children, adults)
        self.last_move = None

    def end(self, line, num):
        """Check that everyone is on Molokai."""
        if not self.started:
            BoatGrader.unusable('This file does not seem to be a log file.')
        elif self.people[ADULT, OAHU] or self.people[CHILD, OAHU]:
            BoatGrader.fail(line, num, str(self.people))
        else:
            print('Success! All {} people have been successfully moved to Molokai.'.format(self.people.total))

    def move(self, line, num):
        """Parse, carry out, and check the validity of a move."""
        if not self.started:
            BoatGrader.unusable('Must have a declaration line "Testing Boats..."')
        move = BoatGrader.parse_move(line) # Parse
        age, movetype, destination = move
        origin = otherisland(destination)

        # Check validity
        BoatGrader.check_successor(self.last_move, move, line, num)
        if self.people[age, origin] == 0:
            BoatGrader.fail(line, num,
                            '[constraint 1] There are no {} available to move to {}.'.format(plural(age), destination))

        # Carry out
        self.people.movefrom(age, origin)

        self.last_move = move

    @staticmethod
    def parse_move(line):
        """A move is age, move type, and destination"""
        move = []
        if 'Child' in line:   move.append(CHILD)
        else:                 move.append(ADULT)
        if 'rowing' in line:  move.append(ROW)
        else:                 move.append(RIDE)
        if 'Oahu' in line:    move.append(OAHU)
        else:                 move.append(MOLOKAI)
        return tuple(move)

    @staticmethod
    def check_successor(prev, curr, line, num):
        if curr not in successor_table[prev]:
            if prev is None:
                BoatGrader.fail(line, num, '[constraint 2] {} is not a legal first move'.format(move_str(curr)))
            else:
                BoatGrader.fail(line, num, '[constraint 2] {} is not a legal successor of {}.'.format(move_str(curr), move_str(prev)))

    @staticmethod
    def fail(line, num, errmsg):
        """This method is called when an error is found. It prints the line,
        line number, and an error message, and then stops the test."""
        if num == -1:
            print('Failure: [constraint 0] Simulation ended without everyone on Molokai.')
        else:
            print('Failure at line {}, which is:'.format(num), line, end='')
        print(errmsg)
        raise StopTest()

    @staticmethod
    def unusable(errmsg):
        print('Unusable log file: ' + errmsg)
        raise StopTest()

    @staticmethod
    def printline(line, num):
        """Print a line from the log file with its line number and without an
        extra newline. (assuming the line already ends with a newline)"""
        print(str(num).rjust(4), line, end='')

class Population:
    """A Population keeps track of where adults and children are, giving a
    simple interface for querying the number of people on an island and moving
    a person from one island to another."""

    def __init__(self, children, adults):
        """Create a population with specified numbers of people (all on
        Oahu)."""
        self._people = {}
        self._people[ADULT] = adults
        self._people[CHILD] = children
        self._people[ADULT, OAHU] = adults
        self._people[CHILD, OAHU] = children
        self.total = children + adults

    def __getitem__(self, key):
        """Get the number of people of the specified age on the specified
        island. The key is a tuple (age, island).

        >>> pop = Population(3, 4)
        >>> pop[ADULT, OAHU] # how many adults are on Oahu
        4
        """
        age, island = key
        if island == OAHU:
            return self._people[age, OAHU]
        else:
            return self._people[age] - self._people[age, OAHU]

    def movefrom(self, age, island):
        """Move one person of the specified age from the specified island."""
        if island == OAHU:
            self._people[age, OAHU] -= 1
        else:
            self._people[age, OAHU] += 1

    def __repr__(self):
        return 'Oahu has {}/{} adults and {}/{} children.'.format(
                   self._people[ADULT, OAHU],
                   self._people[ADULT],
                   self._people[CHILD, OAHU],
                   self._people[CHILD])

### FUNCTIONS TO PROCESS SUCCESSOR TABLE ###

def make_successor_table(raw_successor_table):
    successor_table = {}
    for key in raw_successor_table:
        new_key = parse_abbr_move(key)
        successor_table[new_key] = []
        values = raw_successor_table[key].split(' or ')
        for val in values:
            if val[0] == '*':
                successor_table[new_key].append(parse_abbr_move('c' + val[1:]))
                successor_table[new_key].append(parse_abbr_move('a' + val[1:]))
            else:
                successor_table[new_key].append(parse_abbr_move(val))
    return successor_table

def parse_abbr_move(move):
    """Turn a move abbreviation like cm-ride into a proper move tuple."""
    if move == None:
        return None
    ageletter, tail = move[0], move[1:]
    destination, movetype = tail.split('-')
    age = {'a': ADULT, 'c': CHILD}[ageletter]
    movetype = {'row': ROW, 'ride': RIDE}[movetype]
    destination = {'m': MOLOKAI, 'o': OAHU}[destination]
    return (age, movetype, destination)

### THE CODE THAT WILL ACTUALLY RUN ###

if __name__ == '__main__':
    if len(sys.argv) == 2:
        successor_table = make_successor_table(raw_successor_table)
        run_test(sys.argv[1])
    else:
        print(__doc__)
